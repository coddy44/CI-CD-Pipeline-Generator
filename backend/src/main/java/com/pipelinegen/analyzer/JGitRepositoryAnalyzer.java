package com.pipelinegen.analyzer;

import com.pipelinegen.exception.RepositoryAccessException;
import com.pipelinegen.model.ProjectStack;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("!stub")
public class JGitRepositoryAnalyzer implements RepositoryAnalyzer {
    private final JGitRepositoryClient client;
    private final StackDetector detector;

    public JGitRepositoryAnalyzer(JGitRepositoryClient client, StackDetector detector) {
        this.client = client;
        this.detector = detector;
    }

    @Override
    public ProjectStack analyze(String repoUrl, String branch) {
        try (var clone = client.cloneRepository(repoUrl, branch)) {
            DetectedStack stack = detector.detect(clone.directory());
            requireSupportedBranch(clone.branch());
            String defaultBranch = branch == null || branch.isBlank() ? clone.branch() : client.defaultBranch(repoUrl);
            requireSupportedBranch(defaultBranch);
            return new ProjectStack(stack.language(), stack.languageVersion(), stack.buildTool(), stack.frameworks(),
                    stack.hasTests(), stack.hasDockerfile(), defaultBranch);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать или удалить временную копию репозитория", e);
        }
    }

    private static void requireSupportedBranch(String branch) {
        // Имена веток подставляются в строковые литералы шаблонов CI.
        if (!branch.matches("[A-Za-z0-9._/-]+")) {
            throw new RepositoryAccessException("В имени ветки есть символы, не поддерживаемые шаблонами CI", null);
        }
    }
}
