import { useEffect, useRef, useState } from "react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import type { Pipeline } from "../api/types";

const buttonClassName =
  "rounded-md border border-line bg-raised px-3 py-1.5 text-xs font-medium text-dim transition hover:border-pulsar/50 hover:text-star";

function getLanguage(fileName: string) {
  if (fileName.toLowerCase().includes("jenkins")) return "groovy";
  if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return "yaml";
  return "text";
}

export function PipelineCode({ pipeline }: { pipeline: Pipeline }) {
  const [isCopied, setIsCopied] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(
    () => () => {
      if (timerRef.current) window.clearTimeout(timerRef.current);
    },
    [],
  );

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(pipeline.content);
    } catch {
      const area = document.createElement("textarea");
      area.value = pipeline.content;
      document.body.appendChild(area);
      area.select();
      document.execCommand("copy");
      area.remove();
    }
    setIsCopied(true);
    if (timerRef.current) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => setIsCopied(false), 2000);
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
    <div className="anim-fade-up flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="rounded-md border border-line bg-abyss/80 px-2.5 py-1 font-mono text-xs text-comet">
          {pipeline.fileName}
        </span>
        <div className="flex gap-2">
          <button
            onClick={handleCopy}
            className={`${buttonClassName} ${isCopied ? "!border-aurora/50 !text-aurora" : ""}`}
          >
            {isCopied ? "Скопировано ✓" : "Скопировать"}
          </button>
          <button onClick={handleDownload} className={buttonClassName}>
            Скачать
          </button>
        </div>
      </div>
      <div className="max-h-[420px] overflow-auto rounded-lg border border-line/70 lg:max-h-none">
        <SyntaxHighlighter
          language={getLanguage(pipeline.fileName)}
          style={vscDarkPlus}
          showLineNumbers={true}
          wrapLines={true}
          customStyle={{
            margin: 0,
            padding: "1rem",
            background: "#070b1a",
            fontSize: "0.78rem",
            fontFamily: "'JetBrains Mono', ui-monospace, monospace",
          }}
          lineNumberStyle={{ color: "#3d4a80", minWidth: "2.2em" }}
        >
          {pipeline.content}
        </SyntaxHighlighter>
      </div>
    </div>
  );
}