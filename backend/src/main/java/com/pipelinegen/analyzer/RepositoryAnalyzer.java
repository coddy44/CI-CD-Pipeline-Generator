package com.pipelinegen.analyzer;

import com.pipelinegen.model.ProjectStack;

/**
 * Точка подключения анализатора репозитория.
 * Генератор и API работают только с этим интерфейсом, поэтому реализацию можно заменить,
 * не трогая остальной код.
 */
public interface RepositoryAnalyzer {

    /**
     * @param repoUrl ссылка на репозиторий
     * @param branch  ветка; null — ветка по умолчанию
     * @return определённый стек проекта
     * @throws com.pipelinegen.exception.RepositoryAccessException если репозиторий недоступен
     */
    ProjectStack analyze(String repoUrl, String branch);
}
