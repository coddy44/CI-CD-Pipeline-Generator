package com.pipelinegen.analyzer;

import com.pipelinegen.exception.RepositoryAccessException;
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
import java.nio.file.Files;
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
            @Value("${app.repository.temp-directory:./target/repositories}") Path temporaryDirectory,
            @Value("${app.repository.timeout-seconds:30}") int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Таймаут должен быть положительным");
        }
        this.temporaryDirectory = temporaryDirectory.toAbsolutePath().normalize();
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * @param repoUrl публичный HTTPS-адрес GitHub или GitLab
     * @param branch имя ветки; null или пустая строка — ветка по умолчанию
     * @return временная копия; вызывающий код должен закрыть её через try-with-resources
     * @throws RepositoryAccessException если репозиторий или ветка недоступны
     * @throws IOException если не удалось создать или прочитать временные файлы
     */
    public ClonedRepository cloneRepository(String repoUrl, String branch) throws IOException {
        validateUrl(repoUrl);
        String requestedBranch = validateBranch(branch);

        Files.createDirectories(temporaryDirectory);
        Path directory = Files.createTempDirectory(temporaryDirectory, "repository-");

        CloneCommand command = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(directory.toFile())
                .setCloneAllBranches(false)
                .setCloneSubmodules(false)
                .setNoTags()
                .setDepth(1)
                .setTimeout(timeoutSeconds)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", ""));

        if (requestedBranch != null) {
            String ref = Constants.R_HEADS + requestedBranch;
            command.setBranch(ref).setBranchesToClone(List.of(ref));
        }

        try (Git git = command.call()) {
            if (git.getRepository().resolve(Constants.HEAD) == null) {
                throw new RepositoryAccessException("Репозиторий не содержит коммитов", null);
            }
            return new ClonedRepository(directory, git.getRepository().getBranch());
        } catch (GitAPIException e) {
            deleteAfterFailure(directory, e);
            throw new RepositoryAccessException("Не удалось получить публичный репозиторий или указанную ветку", e);
        } catch (IOException | RuntimeException e) {
            deleteAfterFailure(directory, e);
            throw e;
        }
    }

    private static void validateUrl(String repoUrl) {
        URI uri;
        try {
            uri = URI.create(repoUrl == null ? "" : repoUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный адрес репозитория", e);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                || uri.getRawUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !uri.getRawPath().matches("/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?")
                || !uri.normalize().equals(uri)) {
            throw new IllegalArgumentException("Ожидается HTTPS-ссылка вида https://github.com/owner/repo или https://gitlab.com/owner/repo");
        }
    }

    private static String validateBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return null;
        }
        if (!Repository.isValidRefName(Constants.R_HEADS + branch)) {
            throw new IllegalArgumentException("Некорректное имя ветки");
        }
        return branch;
    }

    private static void deleteAfterFailure(Path directory, Exception failure) {
        try {
            ClonedRepository.deleteDirectory(directory);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
