export function CosmicBackground() {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 z-0 overflow-hidden">
      <div
        className="nebula-drift absolute -left-48 -top-48 h-[65vmax] w-[65vmax] rounded-full opacity-30 blur-3xl"
        style={{ background: "radial-gradient(circle, #16266b 0%, transparent 62%)" }}
      />
      <div
        className="nebula-drift-slow absolute -bottom-56 -right-40 h-[60vmax] w-[60vmax] rounded-full opacity-25 blur-3xl"
        style={{ background: "radial-gradient(circle, #3b1d7a 0%, transparent 62%)" }}
      />
      <div
        className="absolute right-[15%] top-[10%] h-[35vmax] w-[35vmax] rounded-full opacity-15 blur-3xl"
        style={{ background: "radial-gradient(circle, #0e4a5e 0%, transparent 60%)" }}
      />
      <div className="starfield absolute inset-0" />
      <div className="grain absolute inset-0 opacity-5" />
    </div>
  );
}