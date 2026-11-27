package com.pipelinegen.generator;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.*;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplatePipelineGeneratorTest {

    private final TemplatePipelineGenerator generator = new TemplatePipelineGenerator();

    private static ProjectStack javaMaven(boolean hasTests, boolean hasDockerfile) {
        return new ProjectStack(Language.JAVA, "21", BuildTool.MAVEN,
                List.of("spring-boot"), hasTests, hasDockerfile, "main");
    }

    private static ProjectStack typescriptNpm(boolean hasTests, boolean hasDockerfile) {
        return new ProjectStack(Language.TYPESCRIPT, "22", BuildTool.NPM,
                List.of("react"), hasTests, hasDockerfile, "main");
    }

    private static ProjectStack pythonPip(boolean hasTests) {
        return new ProjectStack(Language.PYTHON, "3.12", BuildTool.PIP,
                List.of(), hasTests, false, "main");
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
    void buildsTemplatePathFromStack() {
        assertThat(TemplatePipelineGenerator.templatePath(javaMaven(true, false), CiPlatform.GITLAB_CI))
                .isEqualTo("gitlab-ci/java-maven.yml.ftl");
        assertThat(TemplatePipelineGenerator.templatePath(javaMaven(true, false), CiPlatform.JENKINS))
                .isEqualTo("jenkins/java-maven.groovy.ftl");
    }

    @Test
    void generatesGitlabCiForTypescriptNpm() {
        PipelineFile file = generator.generate(typescriptNpm(true, false), CiPlatform.GITLAB_CI, "main");

        assertThat(file.fileName()).isEqualTo(".gitlab-ci.yml");
        assertThat(file.content())
                .contains("image: node:22")
                .contains("npm ci")
                .contains("npm install")
                .contains("npm run build --if-present")
                .contains("npm run test --if-present");
        assertThat(file.structure().stages()).containsExactly("build");
        assertThat(file.structure().nodes()).extracting(PipelineStructure.Step::id)
                .containsExactly("install", "build", "test");
        assertThat(file.structure().nodes()).extracting(PipelineStructure.Step::needs)
                .containsExactly(List.of(), List.of("install"), List.of("build"));

        Map<String, Object> yaml = new Yaml().load(file.content());
        Map<?, ?> buildJob = (Map<?, ?>) yaml.get("build");
        assertThat(buildJob.get("script")).isEqualTo(file.structure().nodes().stream()
                .map(step -> step.details().command())
                .toList());
    }

    @Test
    void skipsGitlabTestCommandForTypescriptNpmWithoutTests() {
        PipelineFile file = generator.generate(
                typescriptNpm(false, false), CiPlatform.GITLAB_CI, "main");

        assertThat(file.content()).doesNotContain("npm run test");
        assertThat(file.structure().nodes()).extracting(PipelineStructure.Step::id)
                .containsExactly("install", "build");
    }

    @Test
    void generatesJenkinsfileForTypescriptNpm() {
        PipelineFile file = generator.generate(typescriptNpm(true, false), CiPlatform.JENKINS, "main");

        assertThat(file.fileName()).isEqualTo("Jenkinsfile");
        assertThat(file.content())
                .contains("image 'node:22'")
                .contains("npm install")
                .contains("npm run build --if-present")
                .contains("npm run test --if-present");
    }

    @Test
    void skipsJenkinsTestStageForTypescriptNpmWithoutTests() {
        PipelineFile file = generator.generate(typescriptNpm(false, false), CiPlatform.JENKINS, "main");

        assertThat(file.fileName()).isEqualTo("Jenkinsfile");
        assertThat(file.content())
                .doesNotContain("stage('Test')");
    }

    @Test
    void generatesGitlabCiForPythonPip() {
        PipelineFile file = generator.generate(pythonPip(true), CiPlatform.GITLAB_CI, "main");

        assertThat(file.fileName()).isEqualTo(".gitlab-ci.yml");
        assertThat(file.content())
                .contains("image: python:3.12")
                .contains("python -m pip install -r requirements.txt")
                .contains("python -m pip install .")
                .contains("python -m pytest");
    }

    @Test
    void generatesJenkinsfileForPythonPip() {
        PipelineFile file = generator.generate(pythonPip(true), CiPlatform.JENKINS, "main");

        assertThat(file.fileName()).isEqualTo("Jenkinsfile");
        assertThat(file.content())
                .contains("image 'python:3.12'")
                .contains("python -m pip install -r requirements.txt")
                .contains("python -m pip install .")
                .contains("stage('Test')")
                .contains("python -m pytest");
    }

    @Test
    void skipsPythonTestCommandsWhenNoTests() {
        ProjectStack stack = pythonPip(false);

        assertThat(generator.generate(stack, CiPlatform.GITLAB_CI, "main").content())
                .doesNotContain("python -m pytest");
        assertThat(generator.generate(stack, CiPlatform.JENKINS, "main").content())
                .doesNotContain("stage('Test')", "python -m pytest");
    }

    @Test
    void failsForStackWithoutTemplate() {
        ProjectStack javascript = new ProjectStack(Language.JAVASCRIPT, "22", BuildTool.NPM,
                List.of(), true, false, "main");

        assertThatThrownBy(() -> generator.generate(javascript, CiPlatform.GITLAB_CI, "main"))
                .isInstanceOf(UnsupportedStackException.class);
    }
}
