import React from "react";
import ReactFlow from "react-flow-renderer";
import FlowNode from "@Components/FlowDiagram/FlowNode";
import BlockNode from "@Components/FlowDiagram/BlockNode";
import styles from "@Components/SiteHeader/FlowsDropdown/styles.module.scss";
import { Modal } from "@r3/r3-tooling-design-system";
import RequestDraftTransferOfOwnershipFlow from "@Components/FlowDiagram/templates/RequestDraftTransferOfOwnershipFlow";
import OfferEncumberedTokensFlow from "@Components/FlowDiagram/templates/OfferEncumberedTokensFlow";
import SignAndFinalizeTransferOfOwnership from "@Components/FlowDiagram/templates/SignAndFinalizeTransferOfOwnership";
import UnlockEncumberedTokensFlow from "@Components/FlowDiagram/templates/UnlockEncumberedTokensFlow";
import { FlowData } from "@Models";

const nodeTypes = {
  flow: FlowNode,
  block: BlockNode,
};

const templates: { [index: string]: any } = {
  RequestDraftTransferOfOwnershipFlow,
  OfferEncumberedTokensFlow,
  SignAndFinalizeTransferOfOwnership,
  UnlockEncumberedTokensFlow,
};

interface Props {
  selectedFlow: FlowData | null;
  open: boolean;
  onClose: () => void;
}

function FlowDiagram({ selectedFlow, open, onClose }: Props) {
  if (!selectedFlow) return null;

  //@ts-ignore .at() method doesnt seem to be supported in Typescript
  const stageName = selectedFlow.associatedStage.split(".").at(-1);

  const template = templates[stageName] ? templates[stageName](selectedFlow) : [];
  return (
    <Modal
      className={styles.modal}
      onClose={onClose}
      size="large"
      title={stageName.split(/(?=[A-Z])/).join(" ")}
      withBackdrop
      open={open}
    >
      {selectedFlow && <ReactFlow elements={template} nodeTypes={nodeTypes} />}
    </Modal>
  );
}

export default FlowDiagram;
