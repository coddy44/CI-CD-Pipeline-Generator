// frontend/src/components/PipelineGraph.tsx

import { useEffect } from 'react'
import { ReactFlow, Background, Controls, useNodesState, useEdgesState } from '@xyflow/react'
import type { Node, Edge } from '@xyflow/react'
import type { Pipeline } from '../api/types'

// 1. ВЫНОСИМ МОК ЗА ПРЕДЕЛЫ КОМПОНЕНТА
// Теперь этот объект создается один раз при загрузке файла
const MOCK_STRUCTURE = {
  stages: ['build', 'test', 'deploy'],
  nodes: [
    { id: 'checkout', label: 'Checkout', needs: [] },
    { id: 'build', label: 'Build', needs: ['checkout'] },
    { id: 'test', label: 'Test', needs: ['build'] },
    { id: 'deploy', label: 'Deploy', needs: ['test'] },
  ]
}

export function PipelineGraph({ pipeline }: { pipeline: Pipeline }) {
  const structure = pipeline.structure || MOCK_STRUCTURE

  // 2. Создаем начальные узлы и связи (без useMemo, это безопасно при таких объемах)
  const getInitialNodes = (): Node[] => {
    return structure.nodes.map((node, index) => ({
      id: node.id,
      position: { x: 250, y: index * 100 },
      data: { label: node.label },
      style: {
        background: '#fff',
        border: '1px solid #e2e8f0',
        borderRadius: '8px',
        padding: '10px 20px',
        fontWeight: 'bold',
        color: '#334155',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        width: 150,
        textAlign: 'center',
      },
    }))
  }

  const getInitialEdges = (): Edge[] => {
    return structure.nodes.flatMap((node) =>
      (node.needs || []).map((needId) => ({
        id: `e-${needId}-${node.id}`,
        source: needId,
        target: node.id,
        animated: true,
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      }))
    )
  }

  // 3. Хуки состояния
  const [nodes, setNodes, onNodesChange] = useNodesState(getInitialNodes())
  const [edges, setEdges, onEdgesChange] = useEdgesState(getInitialEdges())

  // 4. Обновляем граф, если пользователь сгенерировал новый пайплайн
  useEffect(() => {
    setNodes(getInitialNodes())
    setEdges(getInitialEdges())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pipeline]) // Зависим только от изменения объекта pipeline

  return (
    <div className="h-[500px] w-full rounded-lg border border-slate-200 bg-slate-50">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
      >
        <Background color="#cbd5e1" gap={16} />
        <Controls />
      </ReactFlow>
    </div>
  )
}