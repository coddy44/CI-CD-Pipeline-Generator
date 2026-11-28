package com.pipelinegen.analyzer;

import com.pipelinegen.exception.RepositoryAccessException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

class JGitRepositoryClientTest {
    @TempDir
    Path root;

    @ParameterizedTest
    @ValueSource(strings = {"http://github.com/a/b", "https://example.org/a/b",
            "https://user@github.com/a/b", "https://github.com:443/a/b",
            "https://github.com/a/b?query", "https://github.com/a/..",
            "https://gitlab.com/group/sub/repo", "https://github.com/a/b c"})
    void rejectsInvalidUrl(String url) {
        var client = new JGitRepositoryClient(root.resolve("clones"), 30);
        assertThrows(RepositoryAccessException.class, () -> client.cloneRepository(url, null));
        assertFalse(Files.exists(root.resolve("clones")));
    }

    @Test
    void acceptsGitSuffixAndRejectsInvalidBranchBeforeCloning() {
        var client = new JGitRepositoryClient(root.resolve("clones"), 30);
        String url = "https://github.com/owner/repo.git";
        assertDoesNotThrow(() -> JGitRepositoryClient.validateUrl(url));
        var failure = assertThrows(RepositoryAccessException.class,
                () -> client.cloneRepository(url, "bad..branch"));
        assertEquals("Некорректное имя ветки", failure.getMessage());
        assertFalse(Files.exists(root.resolve("clones")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"other"})
    void fetchesOnlyTheSelectedBranch(String branch) throws Exception {
        String url = "https://github.com/owner/repo";
        Path remoteDirectory = root.resolve("remote");
        try (var remote = Git.init().setDirectory(remoteDirectory.toFile()).setInitialBranch("trunk").call()) {
            commit(remote, "common.txt");
            remote.checkout().setCreateBranch(true).setName("other").call();
            var otherTip = commit(remote, "other.txt");
            remote.checkout().setName("trunk").call();
            var defaultTip = commit(remote, "main.txt");

            String remotePath = remoteDirectory.toString();
            var lsRemote = spy(Git.lsRemoteRepository().setRemote(remotePath));
            var cloneCommand = spy(Git.cloneRepository().setURI(remotePath));
            doReturn(lsRemote).when(lsRemote).setRemote(url);
            doReturn(cloneCommand).when(cloneCommand).setURI(url);

            try (var gitFactory = mockStatic(Git.class, CALLS_REAL_METHODS)) {
                gitFactory.when(Git::lsRemoteRepository).thenReturn(lsRemote);
                gitFactory.when(Git::cloneRepository).thenReturn(cloneCommand);
                var client = new JGitRepositoryClient(root.resolve("clones"), 30);
                try (var clone = client.cloneRepository(url, branch);
                     var cloned = Git.open(clone.directory().toFile())) {
                    boolean useDefault = branch == null;
                    assertEquals(useDefault ? "trunk" : "other", clone.branch());
                    assertEquals(useDefault ? defaultTip : otherTip, cloned.getRepository().resolve("HEAD"));
                    assertNull(cloned.getRepository().exactRef("refs/remotes/origin/" + (useDefault ? "other" : "trunk")));
                    assertFalse(cloned.getRepository().getObjectDatabase().has(useDefault ? otherTip : defaultTip));
                    if (!useDefault) gitFactory.verify(Git::lsRemoteRepository, never());
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resolvesDirectHeadOnlyWhenOneBranchMatches(boolean ambiguous) throws Exception {
        String url = "https://github.com/owner/repo";
        var headTip = ObjectId.fromString("1".repeat(40));
        var otherTip = ambiguous ? headTip : ObjectId.fromString("2".repeat(40));
        Map<String, Ref> refs = Map.of(
                "HEAD", new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, "HEAD", headTip),
                "refs/heads/trunk", new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, "refs/heads/trunk", headTip),
                "refs/heads/other", new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, "refs/heads/other", otherTip));
        var lsRemote = spy(Git.lsRemoteRepository());
        doReturn(refs).when(lsRemote).callAsMap();
        try (var gitFactory = mockStatic(Git.class, CALLS_REAL_METHODS)) {
            gitFactory.when(Git::lsRemoteRepository).thenReturn(lsRemote);
            var client = new JGitRepositoryClient(root.resolve("clones"), 30);
            if (ambiguous) {
                assertThrows(RepositoryAccessException.class, () -> client.cloneRepository(url, null));
                assertFalse(Files.exists(root.resolve("clones")));
            } else {
                assertEquals("trunk", client.defaultBranch(url));
            }
        }
    }

    private static ObjectId commit(Git remote, String filename) throws Exception {
        Files.writeString(remote.getRepository().getWorkTree().toPath().resolve(filename), filename);
        remote.add().addFilepattern(filename).call();
        return remote.commit().setMessage(filename)
                .setAuthor("Test", "test@example.com").setCommitter("Test", "test@example.com").call().getId();
    }

    @Test
    void startupCleanupRemovesAbandonedCloneAndKeepsActiveAndUnmarkedDirectories() throws Exception {
        Path abandoned = Files.createDirectory(root.resolve("repository-abandoned"));
        Path abandonedLock = Files.createFile(root.resolve("repository-abandoned.lock"));
        Path unmarked = Files.createDirectory(root.resolve("repository-unmarked"));

        try (var active = RepositoryWorkspace.create(root)) {
            new JGitRepositoryClient(root, 30).cleanAbandonedClones();
            assertTrue(Files.isDirectory(active.directory()));
            assertTrue(Files.isDirectory(unmarked));
            assertFalse(Files.exists(abandoned));
            assertFalse(Files.exists(abandonedLock));
        }
    }
}
