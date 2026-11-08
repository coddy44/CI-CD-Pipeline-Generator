import { Handle, Position } from '@xyflow/react'
import { MyCustomBuildIcon, MyCustomCheckoutIcon, MyCustomDeployIcon, MyCustomIcon, MyCustomTestIcon } from './Icons'

const getStepAppearance = (label: string) => {
  const l = label.toLowerCase()
  if (l.includes('checkout') || l.includes('clone')) {
    return { color: 'bg-purple-100 text-purple-700 border-purple-200', Icon: MyCustomCheckoutIcon }
  }
  if (l.includes('build')) {
    return { color: 'bg-blue-100 text-blue-700 border-blue-200', Icon: MyCustomBuildIcon }
  }
  if (l.includes('test')) {
    return { color: 'bg-green-100 text-green-700 border-green-200', Icon: MyCustomTestIcon }
  }
  if (l.includes('deploy')) {
    return { color: 'bg-orange-100 text-orange-700 border-orange-200', Icon: MyCustomDeployIcon }
  }
  return { color: 'bg-slate-100 text-slate-700 border-slate-200', Icon: MyCustomIcon }
}

export function CustomPipelineNode({ data }: { data: { label: string } }) {
  const { color, Icon } = getStepAppearance(data.label)

  return (
    <div className={`w-48 rounded-xl border-2 p-3 shadow-sm ${color} relative`}>
      <Handle 
        type="target" 
        position={Position.Top} 
        className="!bg-slate-400 !w-3 !h-3" 
      />

      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white/60 text-xl shadow-sm">
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex flex-col">
          <span className="text-[10px] font-bold uppercase tracking-wider opacity-60">
            Step
          </span>
          <span className="text-sm font-bold truncate">
            {data.label}
          </span>
        </div>
      </div>

      <Handle 
        type="source" 
        position={Position.Bottom} 
        className="!bg-slate-400 !w-3 !h-3" 
      />
    </div>
  )
}