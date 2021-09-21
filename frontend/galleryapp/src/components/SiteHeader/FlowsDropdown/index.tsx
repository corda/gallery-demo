import styles from "./styles.module.scss";
import { ReactComponent as UmlIcon } from "@Assets/umlIcon.svg";
import { Dropdown, Option } from "@r3/r3-tooling-design-system";
import React, { useContext, useState } from "react";
import FlowDiagram from "@Components/FlowDiagram";
import { LogsContext } from "@Context/logs";
import { FlowData } from "@Models";

function FlowsDropdown() {
  const [toggledModal, setToggleModal] = useState(false);
  const [selectedFlow, setSelectedFlow] = useState<FlowData | null>(null);
  const { logs } = useContext(LogsContext);

  const completedFlows = logs.filter((log) => !!log.completed).map((log) => log.completed);

  const handleLogClick = (selectedFlow: any) => {
    setSelectedFlow(selectedFlow);
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
              key={flow!.associatedStage}
              value={flow!.associatedStage}
              onClick={() => {
                handleLogClick(flow);
              }}
            >
              {flow!.associatedStage
                .split(".")
                //@ts-ignore .at() method doesnt seem to be supported in Typescript
                .at(-1)
                .split(/(?=[A-Z])/)
                .join(" ")}
            </Option>
          ))}
        </Dropdown>
      </div>

      <FlowDiagram
        selectedFlow={selectedFlow}
        open={toggledModal}
        onClose={() => setToggleModal(false)}
      />
    </>
  );
}

export default FlowsDropdown;
