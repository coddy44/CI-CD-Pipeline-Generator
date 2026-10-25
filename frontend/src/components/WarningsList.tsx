export function WarningsList({ warnings }: { warnings: string[] }) {
  if (!warnings || warnings.length === 0) return null;

  return (
    <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
      <h3 className="text-sm font-semibold text-amber-800">
        На что стоит обратить внимание:
      </h3>
      <ul className="mt-2 list-inside list-disc text-sm text-amber-700">
        {warnings.map((w, index) => (
          <li key={index}>{w}</li>
        ))}
      </ul>
    </div>
  );
}
