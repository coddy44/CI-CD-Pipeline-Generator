package com.pipelinegen.api.dto;

import com.pipelinegen.model.PipelineFile;
import com.pipelinegen.model.ProjectStack;

import java.util.List;

/**
 * Ответ на генерацию.
 *
 * @param repoUrl       исходная ссылка
 * @param detectedStack что определил анализатор (фронт показывает это пользователю)
 * @param pipeline      сгенерированный файл
 * @param warnings      предупреждения для пользователя (может быть пустым)
 */
public record GenerateResponse(
        String repoUrl,
        ProjectStack detectedStack,
        PipelineFile pipeline,
        List<String> warnings
) {
}
