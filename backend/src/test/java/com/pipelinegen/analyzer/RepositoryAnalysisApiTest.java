package com.pipelinegen.analyzer;

import com.pipelinegen.api.GlobalExceptionHandler;
import com.pipelinegen.api.PipelineController;
import com.pipelinegen.generator.TemplatePipelineGenerator;
import com.pipelinegen.service.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryAnalysisApiTest {
    private static final String URL = "https://github.com/owner/repo";
    @TempDir Path root;
    private JGitRepositoryClient client;
    private MockMvc api;

    @BeforeEach
    void configureApi() {
        client = mock(JGitRepositoryClient.class);
        var analyzer = new JGitRepositoryAnalyzer(client, new StackDetector());
        var service = new PipelineService(analyzer, new TemplatePipelineGenerator());
        api = MockMvcBuilders.standaloneSetup(new PipelineController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void generatesPipelineWithFallbackAndClosesClone() throws Exception {
        var workspace = RepositoryWorkspace.create(root);
        Path directory = workspace.directory();
        try (workspace) {
            Files.writeString(directory.resolve("pom.xml"), "<project><artifactId>app</artifactId></project>");
            Files.createDirectories(directory.resolve("src/main/java"));
            Files.createFile(directory.resolve("src/main/java/App.java"));
            when(client.cloneRepository(URL, "feature/build")).thenReturn(new ClonedRepository(workspace, "feature/build"));
            when(client.defaultBranch(URL)).thenReturn("main");

            api.perform(post("/api/v1/pipelines/generate").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"repoUrl":"https://github.com/owner/repo","branch":"feature/build","ciPlatform":"GITLAB_CI"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.detectedStack.defaultBranch").value("main"))
                    .andExpect(jsonPath("$.detectedStack.languageVersion").value("21"))
                    .andExpect(jsonPath("$.warnings[0]").value("Версия среды не определена. Используется версия 21"))
                    .andExpect(jsonPath("$.pipeline.content").value(
                            org.hamcrest.Matchers.containsString("maven:3.9-eclipse-temurin-21")));
            assertThat(directory).doesNotExist();
        }
    }

    @Test
    void reportsInvalidManifestAs422AndClosesClone() throws Exception {
        var workspace = RepositoryWorkspace.create(root);
        Path directory = workspace.directory();
        try (workspace) {
            Files.writeString(directory.resolve("pom.xml"), "<project>");
            when(client.cloneRepository(URL, null)).thenReturn(new ClonedRepository(workspace, "main"));

            api.perform(post("/api/v1/pipelines/generate").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"repoUrl":"https://github.com/owner/repo","ciPlatform":"GITLAB_CI"}
                                    """))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_STACK"));
            assertThat(directory).doesNotExist();
        }
    }
}
