package com.pipelinegen.service;

import com.pipelinegen.analyzer.RepositoryAnalyzer;
import com.pipelinegen.api.dto.GenerateRequest;
import com.pipelinegen.api.dto.GenerateResponse;
import com.pipelinegen.generator.TemplatePipelineGenerator;
import com.pipelinegen.model.PipelineFile;
import com.pipelinegen.model.ProjectStack;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Основной сценарий: анализ репозитория → генерация пайплайна → ответ клиенту. */
@Service
public class PipelineService {

    private final RepositoryAnalyzer analyzer;
    private final TemplatePipelineGenerator generator;

    public PipelineService(RepositoryAnalyzer analyzer, TemplatePipelineGenerator generator) {
        this.analyzer = analyzer;
        this.generator = generator;
    }

    public GenerateResponse generate(GenerateRequest request) {
        ProjectStack stack = analyzer.analyze(request.repoUrl(), request.branch());

        String branch = hasText(request.branch()) ? request.branch() : stack.defaultBranch();
        PipelineFile pipeline = generator.generate(stack, request.ciPlatform(), branch);

        List<String> warnings = new ArrayList<>();
        if (!stack.hasTests()) {
            warnings.add("Тесты не найдены — шаг запуска тестов не добавлен в пайплайн");
        }

        return new GenerateResponse(request.repoUrl(), stack, pipeline, warnings);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
