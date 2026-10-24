package com.pipelinegen.generator;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.CiPlatform;
import com.pipelinegen.model.Language;
import com.pipelinegen.model.PipelineFile;
import com.pipelinegen.model.ProjectStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplatePipelineGeneratorTest {

    private final TemplatePipelineGenerator generator = new TemplatePipelineGenerator();

    private static ProjectStack javaMaven(boolean hasTests, boolean hasDockerfile) {
        return new ProjectStack(Language.JAVA, "21", BuildTool.MAVEN,
                List.of("spring-boot"), hasTests, hasDockerfile, "main");
    }

    @Test
    void generatesGitlabCiForJavaMaven() {
        PipelineFile file = generator.generate(javaMaven(true, false), CiPlatform.GITLAB_CI, "main");

        assertThat(file.fileName()).isEqualTo(".gitlab-ci.yml");
        assertThat(file.content())
                .contains("image: maven:3.9-eclipse-temurin-21")
                .contains("$CI_COMMIT_BRANCH == \"main\"")
                .contains("mvn -B package")
                .contains("mvn -B test")
                // переменные GitLab должны остаться нетронутыми
                .contains("$CI_PROJECT_DIR");
    }

    @Test
    void generatesJenkinsfileForJavaMaven() {
        PipelineFile file = generator.generate(javaMaven(true, true), CiPlatform.JENKINS, "main");

        assertThat(file.fileName()).isEqualTo("Jenkinsfile");
        assertThat(file.content())
                .contains("checkout scm")
                .contains("sh 'mvn -B test'")
                .contains("when { branch 'main' }")
                // синтаксис Groovy ${...} должен остаться нетронутым
                .contains("${env.BUILD_NUMBER}");
    }

    @Test
    void skipsTestStepWhenNoTests() {
        PipelineFile file = generator.generate(javaMaven(false, false), CiPlatform.GITLAB_CI, "main");

        assertThat(file.content()).doesNotContain("mvn -B test");
    }

    @Test
    void failsForStackWithoutTemplate() {
        ProjectStack python = new ProjectStack(Language.PYTHON, "3.12", BuildTool.PIP,
                List.of(), true, false, "main");

        assertThatThrownBy(() -> generator.generate(python, CiPlatform.GITLAB_CI, "main"))
                .isInstanceOf(UnsupportedStackException.class);
    }

    @Test
    void buildsTemplatePathFromStack() {
        assertThat(TemplatePipelineGenerator.templatePath(javaMaven(true, false), CiPlatform.GITLAB_CI))
                .isEqualTo("gitlab-ci/java-maven.yml.ftl");
        assertThat(TemplatePipelineGenerator.templatePath(javaMaven(true, false), CiPlatform.JENKINS))
                .isEqualTo("jenkins/java-maven.groovy.ftl");
    }
}
