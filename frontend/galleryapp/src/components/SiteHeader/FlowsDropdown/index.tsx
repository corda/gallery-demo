import styles from "./styles.module.scss";
import { ReactComponent as UmlIcon } from "@Assets/umlIcon.svg";
import { Dropdown, Modal, Option } from "@r3/r3-tooling-design-system";
import React, { useContext, useState } from "react";
import templateOne from "@Components/FlowDiagram/templates/one";
import templateTwo from "@Components/FlowDiagram/templates/two";
import templateThree from "@Components/FlowDiagram/templates/three";
import templateFour from "@Components/FlowDiagram/templates/four";
import FlowDiagram from "@Components/FlowDiagram";
import { LogsContext } from "@Context/logs";

const templates: { [index: string]: any } = {
  RequestDraftTransferOfOwnershipFlow: templateOne,
  OfferEncumberedTokensFlow: templateTwo,
  SignAndFinalizeTransferOfOwnership: templateThree,
  UnlockEncumberedTokensFlow: templateFour,
};

function FlowsDropdown() {
  const [toggledModal, setToggleModal] = useState(false);
  const [selectedFlow, setSelectedFlow] = useState<any>();
  const { logs } = useContext(LogsContext);

  const completedFlows = logs
    .filter((log) => !!log.completed)
    .map((log) => {
      //@ts-ignore .at() method doesnt seem to be supported in typescript
      const stageName: string = log.completed!.associatedStage.split(".").at(-1);
      return {
        title: stageName,
        template: templates[stageName](log.completed),
      };
    });

  const handleLogClick = (templateNumber: any) => {
    setSelectedFlow(templateNumber);
    setToggleModal(true);
  };

  return (
    <>
      <div className={styles.main}>
        <Dropdown
          positionX="right"
          positionY="bottom"
          trigger={
            <button className={styles.trigger}>
              <UmlIcon />
            </button>
          }
        >
          {completedFlows.map((flow, i) => (
            <Option
              key={flow.title}
              value={flow.title}
              onClick={() => {
                handleLogClick(flow);
              }}
            >
              {flow.title}
            </Option>
          ))}
        </Dropdown>
      </div>
      {selectedFlow && (
        <Modal
          className={styles.modal}
          closeOnOutsideClick
          onClose={() => setToggleModal(false)}
          size="large"
          title={selectedFlow.title}
          withBackdrop
          open={toggledModal}
        >
          <FlowDiagram template={selectedFlow.template} />
        </Modal>
      )}
    </>
  );
}

export default FlowsDropdown;
