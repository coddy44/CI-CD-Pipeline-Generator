package com.pipelinegen.model;

/**
 * CI-система, для которой генерируется пайплайн.
 * Для каждой платформы известно: папка с шаблонами и имя итогового файла в репозитории.
 */
public enum CiPlatform {

    GITLAB_CI("gitlab-ci", ".gitlab-ci.yml", "yml"),
    JENKINS("jenkins", "Jenkinsfile", "groovy");

    private final String templateDir;
    private final String outputFileName;
    private final String fileExtension;

    CiPlatform(String templateDir, String outputFileName, String fileExtension) {
        this.templateDir = templateDir;
        this.outputFileName = outputFileName;
        this.fileExtension = fileExtension;
    }

    public String templateDir() {
        return templateDir;
    }

    public String outputFileName() {
        return outputFileName;
    }

    public String fileExtension() {
        return fileExtension;
    }
}
