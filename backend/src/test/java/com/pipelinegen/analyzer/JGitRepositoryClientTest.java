package com.pipelinegen.analyzer;

import com.pipelinegen.exception.RepositoryAccessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
