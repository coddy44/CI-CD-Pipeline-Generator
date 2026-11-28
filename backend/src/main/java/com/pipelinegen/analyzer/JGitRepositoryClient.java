package com.pipelinegen.analyzer;

import com.pipelinegen.exception.RepositoryAccessException;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Получает публичный репозиторий для последующего анализа. */
@Component
public class JGitRepositoryClient {

    private static final Set<String> ALLOWED_HOSTS = Set.of("github.com", "gitlab.com");

    private final Path temporaryDirectory;
    private final int timeoutSeconds;

    public JGitRepositoryClient(
            @Value("${app.repository.temp-directory:${java.io.tmpdir}/pipeline-generator}") Path temporaryDirectory,
            @Value("${app.repository.timeout-seconds:30}") int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Таймаут должен быть положительным");
        }
        this.temporaryDirectory = temporaryDirectory.toAbsolutePath().normalize();
        this.timeoutSeconds = timeoutSeconds;
    }

    @PostConstruct
    void cleanAbandonedClones() {
        RepositoryWorkspace.cleanAbandoned(temporaryDirectory);
    }

    String defaultBranch(String repoUrl) {
        validateUrl(repoUrl);
        try {
            var refs = Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .setTimeout(timeoutSeconds)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", ""))
                    .callAsMap();
            var head = refs.get(Constants.HEAD);
            if (head != null && head.isSymbolic() && head.getTarget().getName().startsWith(Constants.R_HEADS)) {
                return head.getTarget().getName().substring(Constants.R_HEADS.length());
            }
            if (head != null && !head.isSymbolic() && head.getObjectId() != null) {
                var matches = refs.values().stream()
                        .filter(ref -> ref.getName().startsWith(Constants.R_HEADS))
                        .filter(ref -> head.getObjectId().equals(ref.getObjectId()))
                        .limit(2).toList();
                if (matches.size() == 1) {
                    return matches.getFirst().getName().substring(Constants.R_HEADS.length());
                }
            }
        } catch (GitAPIException e) {
            throw new RepositoryAccessException("Не удалось определить ветку по умолчанию", e);
        }
        throw new RepositoryAccessException("Репозиторий не сообщил ветку по умолчанию", null);
    }

    /**
     * @param repoUrl публичный HTTPS-адрес GitHub или GitLab
     * @param branch имя ветки; null или пустая строка — ветка по умолчанию
     * @return временная копия; вызывающий код должен закрыть её через try-with-resources
     * @throws RepositoryAccessException если адрес или имя ветки некорректны либо репозиторий недоступен
     * @throws IOException если не удалось создать или прочитать временные файлы
     */
    public ClonedRepository cloneRepository(String repoUrl, String branch) throws IOException {
        validateUrl(repoUrl);
        String requestedBranch = validateBranch(branch);
        if (requestedBranch == null) {
            requestedBranch = defaultBranch(repoUrl);
        }

        RepositoryWorkspace workspace = RepositoryWorkspace.create(temporaryDirectory);
        try {
            return cloneInto(workspace, repoUrl, requestedBranch);
        } catch (GitAPIException e) {
            var failure = new RepositoryAccessException(
                    "Не удалось получить публичный репозиторий или указанную ветку", e);
            deleteAfterFailure(workspace, failure);
            throw failure;
        } catch (IOException | RuntimeException e) {
            deleteAfterFailure(workspace, e);
            throw e;
        }
    }

    private ClonedRepository cloneInto(RepositoryWorkspace workspace, String repoUrl, String requestedBranch)
            throws IOException, GitAPIException {
        String ref = Constants.R_HEADS + requestedBranch;
        CloneCommand command = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(workspace.directory().toFile())
                .setCloneAllBranches(false)
                .setBranch(ref)
                .setBranchesToClone(List.of(ref))
                .setCloneSubmodules(false)
                .setNoTags()
                .setDepth(1)
                .setTimeout(timeoutSeconds)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", ""));

        try (Git git = command.call()) {
            if (git.getRepository().resolve(Constants.HEAD) == null) {
                throw new RepositoryAccessException("Репозиторий не содержит коммитов", null);
            }
            return new ClonedRepository(workspace, git.getRepository().getBranch());
        }
    }

    static void validateUrl(String repoUrl) {
        URI uri;
        try {
            uri = URI.create(repoUrl == null ? "" : repoUrl);
        } catch (IllegalArgumentException e) {
            throw new RepositoryAccessException("Некорректный адрес репозитория", e);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                || uri.getRawUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !uri.getRawPath().matches("/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?")
                || uri.getRawPath().matches(".*/\\.{1,2}(/.*)?")) {
            throw new RepositoryAccessException(
                    "Ожидается HTTPS-ссылка вида https://github.com/owner/repo или https://gitlab.com/owner/repo", null);
        }
    }

    private static String validateBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return null;
        }
        if (!Repository.isValidRefName(Constants.R_HEADS + branch)) {
            throw new RepositoryAccessException("Некорректное имя ветки", null);
        }
        return branch;
    }

    private static void deleteAfterFailure(RepositoryWorkspace workspace, Exception failure) {
        try {
            workspace.close();
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
