import { useState, useEffect } from "react";
import { getSupported } from "../api/client";

interface RepoFormProps {
  onSubmit: (repoUrl: string, ciPlatform: string, branch?: string) => void;
  isLoading: boolean;
}

const fieldClassName =
  "w-full rounded-lg border border-line bg-abyss/70 px-3 py-2 text-sm text-star placeholder:text-faint transition focus:border-pulsar focus:outline-none focus:ring-2 focus:ring-pulsar/25";

export function RepoForm({ onSubmit, isLoading }: RepoFormProps) {
  const [repoUrl, setRepoUrl] = useState("");
  const [branch, setBranch] = useState("");
  const [ciPlatform, setCiPlatform] = useState("");
  const [platforms, setPlatforms] = useState<string[]>([]);
  const [isLoadingPlatforms, setIsLoadingPlatforms] = useState(true);
  const [errorPlatforms, setErrorPlatforms] = useState("");

  useEffect(() => {
    getSupported()
      .then((data) => {
        setPlatforms(data.platforms);
        if (data.platforms.length > 0) {
          setCiPlatform(data.platforms[0]);
        }
      })
      .catch((err) => {
        setErrorPlatforms(err.message);
      })
      .finally(() => {
        setIsLoadingPlatforms(false);
      });
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!repoUrl.trim() || !ciPlatform) return;
    onSubmit(repoUrl.trim(), ciPlatform, branch.trim() || undefined);
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div>
        <label
          htmlFor="repo-url"
          className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-dim"
        >
          Ссылка на GitHub-репозиторий <span className="text-nova">*</span>
        </label>
        <input
          id="repo-url"
          type="url"
          required
          placeholder="https://github.com/owner/repo"
          value={repoUrl}
          onChange={(e) => setRepoUrl(e.target.value)}
          className={fieldClassName}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-1">
        <div>
          <label
            htmlFor="branch"
            className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-dim"
          >
            Ветка (опционально)
          </label>
          <input
            id="branch"
            type="text"
            placeholder="main"
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            className={fieldClassName}
          />
        </div>
        <div>
          <label
            htmlFor="ci-platform"
            className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-dim"
          >
            CI-платформа <span className="text-nova">*</span>
          </label>
          <select
            id="ci-platform"
            required
            value={ciPlatform}
            onChange={(e) => setCiPlatform(e.target.value)}
            disabled={isLoadingPlatforms || platforms.length === 0}
            className={`${fieldClassName} appearance-none pr-8 disabled:cursor-not-allowed disabled:opacity-50`}
            style={{
              backgroundImage:
                "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2393a0d8' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E\")",
              backgroundRepeat: "no-repeat",
              backgroundPosition: "right 0.7rem center",
            }}
          >
            {isLoadingPlatforms && <option value="">Загрузка...</option>}
            {!isLoadingPlatforms && platforms.length === 0 && (
              <option value="">Нет доступных платформ</option>
            )}
            {platforms.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
      </div>

      {errorPlatforms && (
        <p className="text-xs text-redshift">
          Ошибка загрузки платформ: {errorPlatforms}
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading || isLoadingPlatforms || platforms.length === 0}
        className="mt-1 w-full rounded-lg bg-gradient-to-r from-pulsar to-quasar px-4 py-2.5 text-sm font-semibold text-white shadow-[0_0_24px_rgba(91,140,255,0.35)] transition hover:brightness-110 hover:shadow-[0_0_36px_rgba(139,92,246,0.5)] disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none"
      >
        {isLoading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeOpacity="0.25"
                strokeWidth="3"
              />
              <path
                d="M22 12a10 10 0 0 0-10-10"
                stroke="currentColor"
                strokeWidth="3"
                strokeLinecap="round"
              />
            </svg>
            Генерация...
          </span>
        ) : (
          "Сгенерировать пайплайн"
        )}
      </button>
    </form>
  );
}