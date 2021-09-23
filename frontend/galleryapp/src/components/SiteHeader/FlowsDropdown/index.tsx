import styles from "./styles.module.scss";
import { ReactComponent as UmlIcon } from "@Assets/umlIcon.svg";
import { Dropdown, Option } from "@r3/r3-tooling-design-system";
import React, { useContext, useState } from "react";
import FlowDiagram from "@Components/FlowDiagram";
import { LogsContext } from "@Context/logs";
import { FlowData, Log } from "@Models";
import config from "@Config";

function FlowsDropdown() {
  const [toggledModal, setToggleModal] = useState(false);
  const [selectedFlow, setSelectedFlow] = useState<FlowData | null>(null);
  const { logs } = useContext(LogsContext);
  const tmpFlowsObj: { [key: string]: Log } = {};

  logs.forEach((log) => {
    if (!log.completed || log.completed!.associatedStage.includes("Handler")) {
      return;
    }
    tmpFlowsObj[`${log.x500}-${log.timestamp}-${log.network}`] = log;
  });
  const completedFlows = Object.entries(tmpFlowsObj).map((pair) => pair[1]);
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
          {completedFlows
            .sort((a, b) => {
              const timeA = a.timestamp.split(" ")[3];
              const timeB = b.timestamp.split(" ")[3];
              if (timeA === timeB) return 0;
              return timeA < timeB ? -1 : 1;
            })
            .map((flow, i) => (
              <Option
                key={`${flow!.completed!.logRecordId}`}
                value={flow!.completed!.associatedStage}
                onClick={() => {
                  handleLogClick(flow.completed);
                }}
              >
                {flow!
                  .completed!.associatedStage.split(".")
                  //@ts-ignore .at() method doesnt seem to be supported in Typescript
                  .at(-1)
                  .split(/(?=[A-Z])/)
                  .join(" ")}
                <span
                  className={`${styles.label}`}
                  style={{
                    backgroundColor: config.networks[flow.network]?.color,
                  }}
                >
                  {flow.x500}
                </span>
                <span className={styles.timeStamp}>{flow.timestamp.split(" ")[3]}</span>
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
