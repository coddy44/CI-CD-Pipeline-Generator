import { useEffect } from 'react'
import { ReactFlow, Background, Controls, useNodesState, useEdgesState, Panel } from '@xyflow/react'
import type { Node, Edge } from '@xyflow/react'
import dagre from '@dagrejs/dagre'
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
    { id: 'lint', label: 'Run Linter', needs: ['build'] },
    { id: 'deploy', label: 'Deploy App', needs: ['test', 'lint'] },
  ]
}

const NODE_WIDTH = 192
const NODE_HEIGHT = 66

const getLayoutedElements = (nodes: Node[], edges: Edge[], direction = 'LR') => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  
  dagreGraph.setGraph({ rankdir: direction, ranksep: 100, nodesep: 40 })  

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT })
  })

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  dagre.layout(dagreGraph)

  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id)
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - NODE_WIDTH / 2,
        y: nodeWithPosition.y - NODE_HEIGHT / 2,
      },
    }
  })

  return { nodes: layoutedNodes, edges }
}

export function PipelineGraph({ pipeline }: { pipeline: Pipeline }) {
  const structure = pipeline.structure || MOCK_STRUCTURE
  const isMockData = !pipeline.structure

  const getInitialElements = () => {
    const rawNodes: Node[] = structure.nodes.map((node) => ({
      id: node.id,
      type: 'custom',
      position: { x: 0, y: 0 },
      data: { label: node.label },
    }))

    const rawEdges: Edge[] = structure.nodes.flatMap((node) =>
      (node.needs || []).map((needId) => ({
        id: `e-${needId}-${node.id}`,
        source: needId,
        target: node.id,
        animated: true,
        type: 'smoothstep',
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      }))
    )

    return getLayoutedElements(rawNodes, rawEdges)
  }

  const initialElements = getInitialElements()
  const [nodes, setNodes, onNodesChange] = useNodesState(initialElements.nodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialElements.edges)

  useEffect(() => {
    const elements = getInitialElements()
    setNodes(elements.nodes)
    setEdges(elements.edges)
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
        {isMockData && (
          <Panel position="top-center" className="m-4">
            <div className="flex items-center gap-2 border border-amber-400 bg-amber-50 px-1 py-2 text-sm font-medium text-amber-800 shadow-sm backdrop-blur-md bg-opacity-90">
              <span>Визуализация на основе моковых данных</span>
            </div>
          </Panel>
        )}
        <Controls />
      </ReactFlow>
    </div>
  )
}