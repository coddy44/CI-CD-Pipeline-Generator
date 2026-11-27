export function WarningsList({ warnings }: { warnings: string[] }) {
  if (!warnings || warnings.length === 0) return null;

  return (
    <div className="anim-fade-up rounded-lg border border-flare/25 bg-flare/10 p-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-flare">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="h-4 w-4 shrink-0"
        >
          <path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
          <path d="M12 9v4" />
          <path d="M12 17h.01" />
        </svg>
        На что стоит обратить внимание
      </h3>
      <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-amber-100/75">
        {warnings.map((w, index) => (
          <li key={index}>{w}</li>
        ))}
      </ul>
    </div>
  );
}