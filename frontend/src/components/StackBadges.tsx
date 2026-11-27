import type { DetectedStack } from "../api/types";

const badgeColors = ["#5b8cff", "#a78bfa", "#22d3ee", "#34d399", "#fbbf24", "#f472b6"];

export function StackBadges({ stack }: { stack: DetectedStack }) {
  const items = [
    `${stack.language} ${stack.languageVersion}`,
    stack.buildTool,
    ...stack.frameworks,
    stack.hasTests ? "Есть тесты" : "Нет тестов",
    stack.hasDockerfile ? "Dockerfile" : null,
  ].filter(Boolean) as string[];

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((label, index) => {
        const color = badgeColors[index % badgeColors.length];
        return (
          <span
            key={label}
            className="anim-fade-up flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium backdrop-blur-md"
            style={{
              color,
              borderColor: `${color}40`,
              backgroundColor: `${color}14`,
              animationDelay: `${index * 60}ms`,
            }}
          >
            <span
              className="h-1.5 w-1.5 rounded-full"
              style={{ backgroundColor: color, boxShadow: `0 0 8px ${color}` }}
            />
            {label}
          </span>
        );
      })}
    </div>
  );
}