package com.pipelinegen.model;

import java.util.List;

/**
 * Результат анализа репозитория — то, что анализатор (backend1) передаёт генератору (backend2).
 * Черновик формата, согласовывается с backend1.
 *
 * @param language        основной язык проекта
 * @param languageVersion версия языка/рантайма (например "21" для Java, "20" для Node, "3.12" для Python)
 * @param buildTool       система сборки
 * @param frameworks      найденные фреймворки (например "spring-boot", "react", "fastapi"); может быть пустым
 * @param hasTests        найдены ли тесты (src/test, *.test.ts, tests/ и т.п.)
 * @param hasDockerfile   есть ли Dockerfile в корне
 * @param defaultBranch   ветка по умолчанию (main/master)
 * @param hasNpmLockfile  найден package-lock.json или npm-shrinkwrap.json
 * @param hasNpmTestScript есть непустой scripts.test без стандартной заглушки npm
 */
public record ProjectStack(
        Language language,
        String languageVersion,
        BuildTool buildTool,
        List<String> frameworks,
        boolean hasTests,
        boolean hasDockerfile,
        String defaultBranch,
        boolean hasNpmLockfile,
        boolean hasNpmTestScript
) {
    public ProjectStack(Language language, String languageVersion, BuildTool buildTool, List<String> frameworks,
                        boolean hasTests, boolean hasDockerfile, String defaultBranch) {
        this(language, languageVersion, buildTool, frameworks, hasTests, hasDockerfile, defaultBranch, false, false);
    }

    public ProjectStack {
        frameworks = frameworks == null ? List.of() : List.copyOf(frameworks);
    }
}
