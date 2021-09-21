import { Dropdown, IconCustom, Option } from "@r3/r3-tooling-design-system";
import React from "react";
import {resetInit} from "@Api";

function SettingsDropdown() {
  return (
    <div>
      <Dropdown
        positionX="right"
        positionY="bottom"
        trigger={
            <IconCustom className="h-5" icon="Cog" />
        }
      >
        <Option
          value="Reset"
          onClick={() => { resetInit()}}
        >
          Reset Data
        </Option>
      </Dropdown>
    </div>
  );
}

export default SettingsDropdown;
