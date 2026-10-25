import type { Pipeline } from "../api/types";

export function PipelineCode({ pipeline }: { pipeline: Pipeline }) {
  const handleCopy = () => {
    navigator.clipboard.writeText(pipeline.content);
    alert("Код скопирован в буфер обмена!");
  };

  const handleDownload = () => {
    const blob = new Blob([pipeline.content], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = pipeline.fileName;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-2">
        <button
          onClick={handleCopy}
          className="rounded bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-200"
        >
          Скопировать
        </button>
        <button
          onClick={handleDownload}
          className="rounded bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-200"
        >
          Скачать {pipeline.fileName}
        </button>
      </div>

      <pre className="max-h-[500px] overflow-auto rounded-lg bg-slate-900 p-4 text-sm text-slate-100">
        <code>{pipeline.content}</code>
      </pre>
    </div>
  );
}
