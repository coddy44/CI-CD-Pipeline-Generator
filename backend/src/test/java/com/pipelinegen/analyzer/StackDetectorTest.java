package com.pipelinegen.analyzer;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StackDetectorTest {
    private static final String POM = "<project><artifactId>app</artifactId>%s</project>";
    private final StackDetector detector = new StackDetector();
    @TempDir Path root;

    @Test
    void readsMavenMetadataAndIgnoresUnrelatedYarnProject() throws IOException {
        write("pom.xml", "\uFEFF" + POM.formatted("""
                <properties><maven.compiler.release>17</maven.compiler.release></properties>
                <dependencies><dependency><groupId>org.springframework.boot</groupId></dependency></dependencies>
                """));
        write("src/main/java/App.java", "");
        write("src/test/java/AppTest.java", "");
        write("Dockerfile", "");
        write("package.json", "{}");
        write("yarn.lock", "");
        var stack = detector.detect(root);
        assertThat(stack).isEqualTo(new DetectedStack(Language.JAVA, BuildTool.MAVEN, "17",
                java.util.List.of("spring-boot"), true, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"js", "ts"})
    void detectsNpmWithBom(String extension) throws IOException {
        write("package.json", "\uFEFF" + """
                {"engines":{"node":"22"},"dependencies":{"react":"*"},"scripts":{"test":"vitest"}}
                """);
        write("package-lock.json", "{}");
        write("src/app." + extension, "");
        var stack = detector.detect(root);
        assertThat(stack.language()).isEqualTo(extension.equals("ts") ? Language.TYPESCRIPT : Language.JAVASCRIPT);
        assertThat(stack.buildTool()).isEqualTo(BuildTool.NPM);
        assertThat(stack.languageVersion()).isEqualTo("22");
        assertThat(stack.frameworks()).containsExactly("react");
        assertThat(stack.hasTests()).isTrue();
        assertThat(stack.hasNpmTestScript()).isTrue();
        assertThat(stack.hasNpmLockfile()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"yarn.lock", "pnpm-lock.yaml"})
    void rejectsOtherPackageManagers(String marker) throws IOException {
        write("package.json", "{}");
        write("index.js", "");
        write(marker, "");
        rejects(marker);
    }

    @ParameterizedTest
    @ValueSource(strings = {"requirements", "pyproject", "setup"})
    void detectsPipProjects(String type) throws IOException {
        write("src/app.py", "");
        switch (type) {
            case "requirements" -> {
                write("requirements.txt", "Flask>=3.0");
                write("poetry.lock", "");
            }
            case "pyproject" -> write("pyproject.toml", """
                    [project]
                    requires-python = "==3.12.4"
                    dependencies = ["flask"]
                    """);
            case "setup" -> write("setup.py", "");
        }
        var stack = detector.detect(root);
        assertThat(stack.language()).isEqualTo(Language.PYTHON);
        assertThat(stack.buildTool()).isEqualTo(BuildTool.PIP);
        if (!type.equals("setup")) assertThat(stack.frameworks()).containsExactly("flask");
        if (type.equals("pyproject")) assertThat(stack.languageVersion()).isEqualTo("3.12.4");
    }

    @Test
    void rejectsPoetryOnly() throws IOException {
        write("pyproject.toml", "[tool.poetry]\nname = 'app'");
        write("app.py", "");
        rejects("Poetry");
    }

    @Test
    void rejectsMultiModuleMaven() throws IOException {
        write("pom.xml", POM.formatted("<modules><module>app</module></modules>"));
        rejects("Многомодульный");
    }

    @Test
    void detectsMavenWithAuxiliaryJavaScript() throws IOException {
        write("pom.xml", POM.formatted(""));
        write("src/main/java/App.java", "");
        write("package.json", "{\"scripts\":{\"docs\":\"node scripts/docs.js\"}}");
        write("scripts/docs.js", "");

        var stack = detector.detect(root);
        assertThat(stack.language()).isEqualTo(Language.JAVA);
        assertThat(stack.buildTool()).isEqualTo(BuildTool.MAVEN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"packageManager", "package-lock.json", "npm-shrinkwrap.json"})
    void rejectsTwoUsableProjects(String marker) throws IOException {
        write("pom.xml", POM.formatted(""));
        write("src/main/java/App.java", "");
        if (marker.equals("packageManager")) {
            write("package.json", "{\"packageManager\":\"npm@10.0.0\"}");
        } else {
            write("package.json", "{}");
            write(marker, "{}");
        }
        write("src/index.js", "");
        rejects("несколько");
    }

    @Test
    void explainsNestedProject() throws IOException {
        write("backend/pom.xml", POM.formatted(""));
        rejects("вложенной");
    }

    @Test
    void rejectsEmptyRepository() {
        rejects("стек не найден");
    }

    @Test
    void rejectsBrokenPom() throws IOException {
        write("pom.xml", "<project>");
        rejects("pom.xml");
    }

    @Test
    void rejectsOversizedManifest() throws IOException {
        write("pom.xml", " ".repeat(1_048_577));
        rejects("1 МиБ");
    }

    @Test
    void rejectsMalformedUtf8() throws IOException {
        Files.write(root.resolve("pom.xml"), new byte[]{(byte) 0xff});
        rejects("UTF-8");
    }

    @Test
    void selectsPythonFromRange() throws IOException {
        write("pyproject.toml", "[project]\nrequires-python = '>=3.13,<3.14'");
        write("app.py", "");
        assertThat(detector.detect(root).languageVersion()).isEqualTo("3.13");
        write(".python-version", "3.12");
        rejects("requires-python");
    }

    @Test
    void comparesExactPythonVersionsNumerically() throws IOException {
        write("pyproject.toml", "[project]\nrequires-python = '==3.13'");
        write("app.py", "");
        write(".python-version", "3.13.0");
        assertThat(detector.detect(root).languageVersion()).isEqualTo("3.13.0");

        write(".python-version", "3.13.1");
        rejects("==3.13");
    }

    @Test
    void rejectsUnsupportedPythonConstraint() throws IOException {
        write("pyproject.toml", "[project]\nrequires-python = '~=3.13'");
        write("app.py", "");
        rejects("~=3.13");
    }

    @ParameterizedTest
    @ValueSource(strings = {"package-lock.json", "npm-shrinkwrap.json", "none"})
    void distinguishesNpmTestFilesFromTestScript(String lockfile) throws IOException {
        write("package.json", "{\"scripts\":{\"test:unit\":\"vitest\"}}");
        write("index.test.ts", "");
        if (!lockfile.equals("none")) write(lockfile, "{}");
        var stack = detector.detect(root);
        assertThat(stack.hasTests()).isTrue();
        assertThat(stack.hasNpmTestScript()).isFalse();
        assertThat(stack.hasNpmLockfile()).isEqualTo(!lockfile.equals("none"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "echo \"Error: no test specified\" && exit 1"})
    void ignoresEmptyAndPlaceholderNpmTestScript(String script) throws IOException {
        write("package.json", tools.jackson.databind.json.JsonMapper.builder().build()
                .writeValueAsString(java.util.Map.of("scripts", java.util.Map.of("test", script))));
        write("index.js", "");
        assertThat(detector.detect(root).hasNpmTestScript()).isFalse();
    }

    private void rejects(String message) {
        assertThatThrownBy(() -> detector.detect(root))
                .isInstanceOf(UnsupportedStackException.class).hasMessageContaining(message);
    }

    private void write(String name, String content) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
