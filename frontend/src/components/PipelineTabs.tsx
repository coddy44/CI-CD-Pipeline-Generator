import { useState } from "react";
import type { Pipeline } from "../api/types";
import { PipelineGraph } from "./PipelineGraph";
import { PipelineCode } from "./PipelineCode";

export function PipelineTabs({ pipeline }: { pipeline: Pipeline }) {
  const [activeTab, setActiveTab] = useState<"graph" | "code">("graph");

  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-4 pt-4">
        <div className="flex gap-6">
          <button
            onClick={() => setActiveTab("graph")}
            className={`pb-3 text-sm font-medium transition-colors ${
              activeTab === "graph"
                ? "border-b-2 border-blue-600 text-blue-600"
                : "text-slate-500 hover:text-slate-700"
            }`}
          >
            Визуальная схема
          </button>
          <button
            onClick={() => setActiveTab("code")}
            className={`pb-3 text-sm font-medium transition-colors ${
              activeTab === "code"
                ? "border-b-2 border-blue-600 text-blue-600"
                : "text-slate-500 hover:text-slate-700"
            }`}
          >
            Исходный код ({pipeline.fileName})
          </button>
        </div>
      </div>

      <div className="p-4">
        {activeTab === "graph" ? (
          <PipelineGraph pipeline={pipeline} />
        ) : (
          <PipelineCode pipeline={pipeline} />
        )}
      </div>
    </div>
  );
}
