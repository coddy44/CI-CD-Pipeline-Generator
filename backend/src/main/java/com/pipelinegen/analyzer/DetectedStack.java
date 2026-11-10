package com.pipelinegen.analyzer;

import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;

/** Результат определения стека; дополнительные параметры ProjectStack заполняются позже. */
public record DetectedStack(Language language, BuildTool buildTool) {
}
