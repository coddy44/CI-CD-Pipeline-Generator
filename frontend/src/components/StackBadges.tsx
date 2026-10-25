import type { DetectedStack } from "../api/types";

export function StackBadges({ stack }: { stack: DetectedStack }) {
  const items = [
    `${stack.language} ${stack.languageVersion}`,
    stack.buildTool,
    ...stack.frameworks,
    stack.hasTests ? "Есть тесты" : "Нет тестов",
    stack.hasDockerfile ? "Dockerfile" : null,
  ].filter(Boolean);

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((label) => (
        <span
          key={label as string}
          className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10"
        >
          {label}
        </span>
      ))}
    </div>
  );
}
