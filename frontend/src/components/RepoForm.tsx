import { useState, useEffect } from "react";
import { getSupported } from "../api/client";

interface RepoFormProps {
  onSubmit: (repoUrl: string, ciPlatform: string, branch?: string) => void;
  isLoading: boolean;
}

export function RepoForm({ onSubmit, isLoading }: RepoFormProps) {
  const [repoUrl, setRepoUrl] = useState("");
  const [branch, setBranch] = useState("");
  const [ciPlatform, setCiPlatform] = useState("");

  const [platforms, setPlatforms] = useState<string[]>([]);
  const [isLoadingPlatforms, setIsLoadingPlatforms] = useState(true);
  const [errorPlatforms, setErrorPlatforms] = useState("");

  // Загрузка платформ при монтировании компонента и выбор первой по умолчанию
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
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Ссылка на GitHub-репозиторий <span className="text-red-500">*</span>
        </label>
        <input
          type="url"
          required
          placeholder="https://github.com/owner/repo"
          value={repoUrl}
          onChange={(e) => setRepoUrl(e.target.value)}
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Ветка (опционально)
          </label>
          <input
            type="text"
            placeholder="main"
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            CI-платформа <span className="text-red-500">*</span>
          </label>
          <select
            required
            value={ciPlatform}
            onChange={(e) => setCiPlatform(e.target.value)}
            disabled={isLoadingPlatforms || platforms.length === 0}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-slate-50 disabled:text-slate-400"
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
        <p className="text-sm text-red-600">
          Ошибка загрузки платформ: {errorPlatforms}
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading || isLoadingPlatforms || platforms.length === 0}
        className="mt-2 w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700 disabled:bg-blue-300"
      >
        {isLoading ? "Генерация..." : "Сгенерировать пайплайн"}
      </button>
    </form>
  );
}
