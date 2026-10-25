export type CiPlatform = string;

export interface GenerateRequest {
  repoUrl: string;
  branch?: string;
  ciPlatform: CiPlatform;
}

export interface SupportedResponse {
  platforms: string[];
  stacks: { language: string; buildTool: string }[];
}

export interface DetectedStack {
  language: string;
  languageVersion: string;
  buildTool: string;
  frameworks: string[];
  hasTests: boolean;
  hasDockerfile: boolean;
  defaultBranch: string;
}

export interface PipelineStep {
  id: string;
  label: string;
  stage: string;
  actionType: string;
  needs: string[];
  details: Record<string, unknown>;
}

export interface PipelineStructure {
  stages: string[];
  nodes: PipelineStep[];
}

export interface Pipeline {
  platform: CiPlatform;
  fileName: string;
  content: string;
  structure?: PipelineStructure;
}

export interface GenerateResponse {
  repoUrl: string;
  detectedStack: DetectedStack;
  pipeline: Pipeline;
  warnings: string[];
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
  timestamp: string;
}
