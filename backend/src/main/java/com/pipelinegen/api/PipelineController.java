package com.pipelinegen.api;

import com.pipelinegen.api.dto.GenerateRequest;
import com.pipelinegen.api.dto.GenerateResponse;
import com.pipelinegen.api.dto.SupportedResponse;
import com.pipelinegen.api.dto.SupportedResponse.StackItem;
import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.CiPlatform;
import com.pipelinegen.model.Language;
import com.pipelinegen.service.PipelineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /** Основной эндпоинт: ссылка на репозиторий → определённый стек + готовый пайплайн. */
    @PostMapping("/pipelines/generate")
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest request) {
        return pipelineService.generate(request);
    }

    /** Список поддерживаемых CI-систем и стеков (стеки MVP, согласованные с backend1). */
    @GetMapping("/supported")
    public SupportedResponse supported() {
        return new SupportedResponse(
                List.of(CiPlatform.values()),
                List.of(
                        new StackItem(Language.JAVA, BuildTool.MAVEN),
                        new StackItem(Language.TYPESCRIPT, BuildTool.NPM),
                        new StackItem(Language.PYTHON, BuildTool.PIP)
                )
        );
    }
}
