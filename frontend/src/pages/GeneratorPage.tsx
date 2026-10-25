import { useState } from "react";
import { generatePipeline, BackendError } from "../api/client";
import type { GenerateResponse } from "../api/types";
import { RepoForm } from "../components/RepoForm";

export function GeneratorPage() {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<GenerateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async (
    repoUrl: string,
    ciPlatform: string,
    branch?: string,
  ) => {
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await generatePipeline({ repoUrl, ciPlatform, branch });
      setResult(data);
    } catch (err) {
      if (err instanceof BackendError) {
        setError(
          `${err.message}${err.details.length ? ": " + err.details.join(", ") : ""}`,
        );
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Произошла неизвестная ошибка");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-8 p-6">
      <header className="text-center">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          CI/CD Pipeline Generator
        </h1>
        <p className="mt-2 text-sm text-slate-500">
          Вставьте ссылку на репозиторий, чтобы автоматически сгенерировать
          конфигурацию пайплайна.
        </p>
      </header>

      <RepoForm onSubmit={handleGenerate} isLoading={isLoading} />

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <strong className="font-semibold">Ошибка:</strong> {error}
        </div>
      )}

      {result && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">
            Результат
          </h2>
          <pre className="max-h-96 overflow-auto rounded-lg bg-slate-900 p-4 text-xs text-slate-100">
            {JSON.stringify(result, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
