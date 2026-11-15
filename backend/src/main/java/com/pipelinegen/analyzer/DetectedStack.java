package com.pipelinegen.analyzer;

import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;
import java.util.List;

/** Свойства проекта, определённые по файлам. */
public record DetectedStack(Language language, BuildTool buildTool, String languageVersion,
                            List<String> frameworks, boolean hasTests, boolean hasDockerfile) {
}
