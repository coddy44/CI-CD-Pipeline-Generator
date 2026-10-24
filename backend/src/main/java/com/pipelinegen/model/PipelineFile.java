package com.pipelinegen.model;

/**
 * Сгенерированный файл пайплайна.
 *
 * @param platform CI-система
 * @param fileName куда положить файл в репозитории (например ".gitlab-ci.yml" или "Jenkinsfile")
 * @param content  текст файла
 */
public record PipelineFile(CiPlatform platform, String fileName, String content) {
}
