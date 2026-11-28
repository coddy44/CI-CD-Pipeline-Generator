package com.pipelinegen.analyzer;

import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;
import com.pipelinegen.model.ProjectStack;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * ВРЕМЕННАЯ заглушка анализатора для итерации 0: репозиторий не клонирует,
 * для любой ссылки возвращает Java/Maven-проект.
 * Нужна, чтобы API и генерацию можно было проверить до готовности настоящего анализатора.
 * Удалить, когда появится реализация на JGit.
 */
@Component
@Profile("stub")
public class StubRepositoryAnalyzer implements RepositoryAnalyzer {

    @Override
    public ProjectStack analyze(String repoUrl, String branch) {
        return new ProjectStack(
                Language.JAVA,
                "21",
                BuildTool.MAVEN,
                List.of("spring-boot"),
                true,
                false,
                "main"
        );
    }
}
