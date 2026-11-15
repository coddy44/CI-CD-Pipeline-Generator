import { useEffect } from "react";
import {
  Background,
  BackgroundVariant,
  Controls,
  MarkerType,
  Panel,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
} from "@xyflow/react";
import type { Edge, Node } from "@xyflow/react";
import dagre from "@dagrejs/dagre";
import type { DetectedStack, Pipeline } from "../api/types";
import { CustomPipelineNode } from "./CustomPipelineNode";
import { StackBadges } from "./StackBadges";

const nodeTypes = { custom: CustomPipelineNode };

const MOCK_STRUCTURE = {
  stages: ["build", "test", "deploy"],
  nodes: [
    { id: "checkout", label: "Checkout", needs: [] },
    { id: "build", label: "Build Project", needs: ["checkout"] },
    { id: "test", label: "Run Tests", needs: ["build"] },
    { id: "lint", label: "Run Linter", needs: ["build"] },
    { id: "deploy", label: "Deploy App", needs: ["test", "lint"] },
  ],
};

const NODE_WIDTH = 192;
const NODE_HEIGHT = 68;

interface StructureNode {
  id: string;
  label: string;
  needs?: string[];
}

interface PipelineGraphProps {
  pipeline: Pipeline | null;
  stack: DetectedStack | null;
  isLoading: boolean;
}

function getLayoutedElements(nodes: Node[], edges: Edge[], direction = "LR") {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));
  dagreGraph.setGraph({ rankdir: direction, ranksep: 100, nodesep: 40 });
  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
  });
  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });
  dagre.layout(dagreGraph);
  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - NODE_WIDTH / 2,
        y: nodeWithPosition.y - NODE_HEIGHT / 2,
      },
    };
  });
  return { nodes: layoutedNodes, edges };
}

function buildElements(structure: { nodes: StructureNode[] }) {
  const rawNodes: Node[] = structure.nodes.map((node) => ({
    id: node.id,
    type: "custom",
    position: { x: 0, y: 0 },
    data: { label: node.label },
  }));
  const rawEdges: Edge[] = structure.nodes.flatMap((node) =>
    (node.needs ?? []).map((needId) => ({
      id: `e-${needId}-${node.id}`,
      source: needId,
      target: node.id,
      animated: true,
      type: "smoothstep",
      style: { stroke: "#4c5fd7", strokeWidth: 2 },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: "#5b8cff",
        width: 16,
        height: 16,
      },
    })),
  );
  return getLayoutedElements(rawNodes, rawEdges);
}

export function PipelineGraph(props: PipelineGraphProps) {
  return (
    <ReactFlowProvider>
      <GraphCanvas {...props} />
    </ReactFlowProvider>
  );
}

function GraphCanvas({ pipeline, stack, isLoading }: PipelineGraphProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const isMockData = pipeline ? !pipeline.structure : false;

  useEffect(() => {
    if (!pipeline) {
      setNodes([]);
      setEdges([]);
      return;
    }
    const elements = buildElements(pipeline.structure ?? MOCK_STRUCTURE);
    setNodes(elements.nodes);
    setEdges(elements.edges);
  }, [pipeline, setNodes, setEdges]);

  return (
    <div className="relative h-full w-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.25 }}
        minZoom={0.2}
        maxZoom={2.5}
        nodesConnectable={false}
        deleteKeyCode={null}
      >
        <Background
          variant={BackgroundVariant.Dots}
          color="#26336b"
          gap={26}
          size={1.4}
        />
        <Controls position="top-right" showInteractive={false} />
        {stack && (
          <Panel position="top-left" className="m-3 max-w-[min(70%,560px)]">
            <StackBadges stack={stack} />
          </Panel>
        )}
        {isMockData && (
          <Panel position="bottom-center" className="mb-3">
            <div className="rounded-full border border-flare/25 bg-abyss/80 px-3 py-1 text-[11px] text-flare/80 backdrop-blur-md">
              Визуализация на основе моковых данных
            </div>
          </Panel>
        )}
        <FitViewOnChange pipeline={pipeline} />
      </ReactFlow>
      {!pipeline && !isLoading && <EmptyState />}
      {isLoading && <LoadingOverlay />}
    </div>
  );
}

function FitViewOnChange({ pipeline }: { pipeline: Pipeline | null }) {
  const { fitView } = useReactFlow();

  useEffect(() => {
    if (!pipeline) return;
    const timer = window.setTimeout(() => {
      fitView({ padding: 0.25, duration: 500 });
    }, 80);
    return () => window.clearTimeout(timer);
  }, [pipeline, fitView]);

  return null;
}

function EmptyState() {
  return (
    <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
      <div className="anim-fade-up flex flex-col items-center gap-4 text-center">
        <div className="relative h-20 w-20">
          <div className="absolute inset-0 rounded-full border border-pulsar/30" />
          <div className="absolute inset-3 rounded-full border border-quasar/30" />
          <div className="absolute inset-0 animate-[orbit_6s_linear_infinite]">
            <span className="absolute left-1/2 top-0 h-2 w-2 -translate-x-1/2 rounded-full bg-comet shadow-[0_0_12px_#22d3ee]" />
          </div>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="h-3 w-3 rounded-full bg-pulsar shadow-[0_0_18px_#5b8cff]" />
          </div>
        </div>
        <div>
          <p className="text-sm font-semibold text-star">Холст пуст</p>
          <p className="mt-1 max-w-[240px] text-xs text-dim">
            Сгенерируй пайплайн — граф сборки появится здесь
          </p>
        </div>
      </div>
    </div>
  );
}

function LoadingOverlay() {
  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-void/50 backdrop-blur-[3px]">
      <div className="flex flex-col items-center gap-4">
        <div className="relative h-14 w-14">
          <div className="absolute inset-0 animate-spin rounded-full border-2 border-pulsar/20 border-t-pulsar" />
          <div className="absolute inset-2 animate-[orbit-reverse_1.6s_linear_infinite] rounded-full border-2 border-quasar/20 border-b-quasar" />
        </div>
        <p className="text-sm text-dim">Анализируем репозиторий…</p>
      </div>
    </div>
  );
}