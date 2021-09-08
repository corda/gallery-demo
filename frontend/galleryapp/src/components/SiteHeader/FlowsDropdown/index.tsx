import styles from "./styles.module.scss";
import { ReactComponent as UmlIcon } from "@Assets/umlIcon.svg";
import { Dropdown, Modal, Option } from "@r3/r3-tooling-design-system";
import React, { useState } from "react";
import templateOne from "@Components/FlowDiagram/templates/one";
import templateTwo from "@Components/FlowDiagram/templates/two";
import templateThree from "@Components/FlowDiagram/templates/three";
import templateFour from "@Components/FlowDiagram/templates/four";
import FlowDiagram from "@Components/FlowDiagram";

const flows = [
  {
    title: "Flow 1",
    template: templateOne,
  },
  {
    title: "Flow 2",
    template: templateTwo,
  },
  {
    title: "Flow 3",
    template: templateThree,
  },
  {
    title: "Flow 4",
    template: templateFour,
  },
];

function FlowsDropdown() {
  const [toggledModal, setToggleModal] = useState(false);
  const [selectedFlow, setSelectedFlow] = useState(0);

  const handleLogClick = (templateNumber: number) => {
    setSelectedFlow(templateNumber);
    setToggleModal(true);
  };

  return (
    <>
      <div className={styles.main}>
        <Dropdown
          closeOnSelectOption
          positionX="right"
          positionY="bottom"
          trigger={
            <button className={styles.trigger}>
              <UmlIcon />
            </button>
          }
        >
          {flows.map((flow, i) => (
            <Option
              value={flow.title}
              onClick={() => {
                handleLogClick(i);
              }}
            >
              {flow.title}
            </Option>
          ))}
        </Dropdown>
      </div>
      <Modal
        className={styles.modal}
        closeOnOutsideClick
        onClose={() => setToggleModal(false)}
        size="large"
        title={flows[selectedFlow].title}
        withBackdrop
        open={toggledModal}
      >
        <FlowDiagram template={flows[selectedFlow].template} />
      </Modal>
    </>
  );
}

export default FlowsDropdown;
