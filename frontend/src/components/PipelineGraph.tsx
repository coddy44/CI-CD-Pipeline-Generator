import { useEffect } from 'react'
import { ReactFlow, Background, Controls, useNodesState, useEdgesState } from '@xyflow/react'
import type { Node, Edge } from '@xyflow/react'
import type { Pipeline } from '../api/types'
import { CustomPipelineNode } from './CustomPipelineNode'

const nodeTypes = {
  custom: CustomPipelineNode,
}

const MOCK_STRUCTURE = {
  stages: ['build', 'test', 'deploy'],
  nodes: [
    { id: 'checkout', label: 'Checkout', needs: [] },
    { id: 'build', label: 'Build Project', needs: ['checkout'] },
    { id: 'test', label: 'Run Tests', needs: ['build'] },
    { id: 'deploy', label: 'Deploy App', needs: ['test'] },
  ]
}

export function PipelineGraph({ pipeline }: { pipeline: Pipeline }) {
  const structure = pipeline.structure || MOCK_STRUCTURE

  const getInitialNodes = (): Node[] => {
    return structure.nodes.map((node, index) => ({
      id: node.id,
      type: 'custom',
      position: { x: 250, y: index * 120 },
      data: { label: node.label },
    }))
  }

  const getInitialEdges = (): Edge[] => {
    return structure.nodes.flatMap((node) =>
      (node.needs || []).map((needId) => ({
        id: `e-${needId}-${node.id}`,
        source: needId,
        target: node.id,
        animated: true,
        type: 'smoothstep',
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      }))
    )
  }

  const [nodes, setNodes, onNodesChange] = useNodesState(getInitialNodes())
  const [edges, setEdges, onEdgesChange] = useEdgesState(getInitialEdges())

  useEffect(() => {
    setNodes(getInitialNodes())
    setEdges(getInitialEdges())
  }, [pipeline])

  return (
    <div className="h-[500px] w-full rounded-lg border border-slate-200 bg-slate-50">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
      >
        <Background color="#cbd5e1" gap={16} />
        <Controls />
      </ReactFlow>
    </div>
  )
}