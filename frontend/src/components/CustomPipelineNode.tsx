import { Handle, Position } from "@xyflow/react";
import {
  MyCustomBuildIcon,
  MyCustomCheckoutIcon,
  MyCustomDeployIcon,
  MyCustomIcon,
  MyCustomTestIcon,
} from "./Icons";

function getStepAppearance(label: string) {
  const l = label.toLowerCase();
  if (l.includes("checkout") || l.includes("clone")) {
    return { accent: "#a78bfa", Icon: MyCustomCheckoutIcon };
  }
  if (l.includes("build")) {
    return { accent: "#5b8cff", Icon: MyCustomBuildIcon };
  }
  if (l.includes("test")) {
    return { accent: "#34d399", Icon: MyCustomTestIcon };
  }
  if (l.includes("deploy")) {
    return { accent: "#fbbf24", Icon: MyCustomDeployIcon };
  }
  return { accent: "#22d3ee", Icon: MyCustomIcon };
}

export function CustomPipelineNode({ data }: { data: { label: string } }) {
  const { accent, Icon } = getStepAppearance(data.label);

  return (
    <div
      className="anim-fade-up relative w-48 rounded-xl border bg-raised/95 p-3 backdrop-blur-sm"
      style={{
        borderColor: `${accent}59`,
        boxShadow: `0 0 22px ${accent}26, 0 10px 28px rgba(2, 4, 12, 0.6)`,
      }}
    >
      <Handle
        type="target"
        position={Position.Left}
        className="!h-3 !w-3 !border-2"
        style={{ background: "#0a1024", borderColor: accent }}
      />
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white/5 ring-1 ring-white/10">
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex min-w-0 flex-col">
          <span
            className="text-[10px] font-bold uppercase tracking-wider"
            style={{ color: accent }}
          >
            Step
          </span>
          <span className="truncate text-sm font-semibold text-star">
            {data.label}
          </span>
        </div>
      </div>
      <Handle
        type="source"
        position={Position.Right}
        className="!h-3 !w-3 !border-2"
        style={{ background: "#0a1024", borderColor: accent }}
      />
    </div>
  );
}