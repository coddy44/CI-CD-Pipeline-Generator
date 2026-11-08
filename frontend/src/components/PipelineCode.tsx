import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
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

  const getLanguage = (fileName: string) => {
    if (fileName.toLowerCase().includes("jenkins")) return "groovy";
    if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return "yaml";
    return "text";
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

      <div className="max-h-[500px] overflow-auto rounded-lg text-sm">
        <SyntaxHighlighter
          language={getLanguage(pipeline.fileName)}
          style={vscDarkPlus}
          showLineNumbers={true}
          wrapLines={true}
          customStyle={{
            margin: 0,
            padding: "1.5rem",
            borderRadius: "0.5rem",
          }}
        >
          {pipeline.content}
        </SyntaxHighlighter>
      </div>
    </div>
  );
}
