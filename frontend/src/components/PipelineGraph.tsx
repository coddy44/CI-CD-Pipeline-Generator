import type { Pipeline } from "../api/types";

export function PipelineGraph({ pipeline }: { pipeline: Pipeline }) {
  if (!pipeline.structure) {
    return (
      <div className="flex h-64 items-center justify-center rounded-lg border-2 border-dashed border-slate-300 bg-slate-50 text-slate-500">
        <p className="text-center max-w-sm">Схема появится в будущем.</p>
      </div>
    );
  }

  return (
    <div className="flex h-64 items-center justify-center rounded-lg border border-slate-200 bg-white">
      <p className="text-slate-500">
        TODO: Здесь будет React Flow с {pipeline.structure.nodes.length} узлами.
      </p>
    </div>
  );
}
