package com.pipelinegen.api.dto;

import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.CiPlatform;
import com.pipelinegen.model.Language;

import java.util.List;

/** Что умеет сервис: для выпадающих списков и подсказок на фронте. */
public record SupportedResponse(List<CiPlatform> platforms, List<StackItem> stacks) {

    public record StackItem(Language language, BuildTool buildTool) {
    }
}
