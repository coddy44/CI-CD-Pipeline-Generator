package com.pipelinegen.api.dto;

import com.pipelinegen.model.CiPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Запрос на генерацию пайплайна.
 *
 * @param repoUrl    ссылка на публичный репозиторий, например https://github.com/user/project
 * @param branch     ветка (необязательно; по умолчанию — ветка по умолчанию репозитория)
 * @param ciPlatform для какой CI-системы генерировать
 */
public record GenerateRequest(
        @NotBlank(message = "Ссылка на репозиторий обязательна")
        @Pattern(regexp = "^https://[\\w.-]+/[\\w.-]+/[\\w.-]+?(\\.git)?/?$",
                message = "Ожидается ссылка вида https://github.com/owner/repo")
        String repoUrl,

        String branch,

        @NotNull(message = "Не выбрана CI-система")
        CiPlatform ciPlatform
) {
}
