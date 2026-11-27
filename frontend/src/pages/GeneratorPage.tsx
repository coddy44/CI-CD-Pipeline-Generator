import { useState } from "react";
import { generatePipeline, BackendError } from "../api/client";
import type { GenerateResponse } from "../api/types";
import { RepoForm } from "../components/RepoForm";
import { WarningsList } from "../components/WarningsList";
import { PipelineGraph } from "../components/PipelineGraph";
import { PipelineCode } from "../components/PipelineCode";
import { CosmicBackground } from "../components/CosmicBackground";

const skeletonWidths = [92, 78, 85, 64, 88, 70, 52];

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
    <div className="relative min-h-dvh">
      <CosmicBackground />

      <div className="relative z-10 grid min-h-dvh grid-cols-1 lg:h-dvh lg:grid-cols-[400px_minmax(0,1fr)] lg:grid-rows-[auto_minmax(0,1fr)]">
        {/* Левая панель: шапка + форма + ошибки + предупреждения */}
        <section className="border-b border-line/60 bg-panel/70 backdrop-blur-xl lg:col-start-1 lg:row-start-1 lg:max-h-[70dvh] lg:overflow-y-auto lg:border-b-0 lg:border-r">
          <header className="flex items-center gap-3 border-b border-line/50 px-5 py-4">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-pulsar/25 to-quasar/25 shadow-[0_0_18px_rgba(91,140,255,0.25)] ring-1 ring-pulsar/40">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                className="h-5 w-5 text-pulsar"
              >
                <circle cx="12" cy="12" r="3.5" />
                <ellipse cx="12" cy="12" rx="10" ry="4.2" transform="rotate(-24 12 12)" />
                <circle cx="19.5" cy="6.5" r="1.2" fill="currentColor" stroke="none" />
              </svg>
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-base font-bold tracking-tight text-star">
                CI/CD Pipeline Generator
              </h1>
              <p className="text-xs text-faint">
                Пайплайн из репозитория за один запуск
              </p>
            </div>
          </header>

          <div className="flex flex-col gap-4 p-5">
            <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-faint">
              Параметры запуска
            </p>
            <RepoForm onSubmit={handleGenerate} isLoading={isLoading} />
            {error && (
              <div className="anim-fade-up rounded-lg border border-redshift/30 bg-redshift/10 p-3 text-sm text-redshift">
                <strong className="font-semibold">Ошибка:</strong> {error}
              </div>
            )}
            {result && <WarningsList warnings={result.warnings} />}
          </div>
        </section>

        {/* Холст: всегда виден, на ПК занимает всё правое пространство */}
        <main
          aria-label="Холст визуализации"
          className="relative h-[440px] lg:col-start-2 lg:row-start-1 lg:row-span-2 lg:h-full"
        >
          <PipelineGraph
            pipeline={result?.pipeline ?? null}
            stack={result?.detectedStack ?? null}
            isLoading={isLoading}
          />
        </main>

        {/* Левая панель снизу: код с копированием и скачиванием */}
        <section className="border-t border-line/60 bg-panel/70 backdrop-blur-xl lg:col-start-1 lg:row-start-2 lg:min-h-0 lg:overflow-y-auto lg:border-r">
          <div className="p-5 pt-4">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-[11px] font-semibold uppercase tracking-[0.22em] text-faint">
                Исходный код
              </h2>
              {result && (
                <span className="rounded-full border border-comet/30 bg-comet/10 px-2 py-0.5 text-[10px] font-medium text-comet">
                  {result.pipeline.platform}
                </span>
              )}
            </div>

            {isLoading ? (
              <div className="flex flex-col gap-2">
                {skeletonWidths.map((width, index) => (
                  <div
                    key={index}
                    className="skeleton h-4 rounded"
                    style={{ width: `${width}%` }}
                  />
                ))}
              </div>
            ) : result ? (
              <PipelineCode pipeline={result.pipeline} />
            ) : (
              <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-line/80 px-4 py-8 text-center">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="h-6 w-6 text-faint"
                >
                  <path d="M8 6l-6 6 6 6" />
                  <path d="M16 6l6 6-6 6" />
                </svg>
                <p className="text-sm text-dim">Здесь появится готовый конфиг</p>
                <p className="text-xs text-faint">
                  Заполни параметры запуска и нажми «Сгенерировать»
                </p>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}