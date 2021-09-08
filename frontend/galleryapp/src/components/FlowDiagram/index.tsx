import React from "react";
import ReactFlow from "react-flow-renderer";
import FlowNode from "@Components/FlowDiagram/FlowNode";
import BlockNode from "@Components/FlowDiagram/BlockNode";

const nodeTypes = {
  flow: FlowNode,
  block: BlockNode,
};

interface Props {
  template: any;
}

function FlowDiagram({ template }: Props) {
  return <ReactFlow elements={template} nodeTypes={nodeTypes} />;
}

export default FlowDiagram;
