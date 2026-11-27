package com.pipelinegen.model;

import java.util.List;

public record PipelineStructure(List<String> stages, List<Step> nodes) {
    public PipelineStructure {
        stages = List.copyOf(stages);
        nodes = List.copyOf(nodes);
    }

    public record Step(
            String id, String label, String stage, String actionType,
            List<String> needs, Details details
    ) {
    }

    public record Details(String command) {
    }
}