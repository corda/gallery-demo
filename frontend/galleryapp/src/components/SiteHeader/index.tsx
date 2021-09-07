import { Dropdown, IconCustom, Option, TopNavBar } from "@r3/r3-tooling-design-system";
import React, { useContext } from "react";
import { useHistory } from "react-router-dom";
import { UsersContext } from "@Context/users";
import { getParticipantPath } from "@Helpers/users";

function SiteHeader() {
  const history = useHistory();
  const { list } = useContext(UsersContext);

  return (
    <TopNavBar
      logoAlt="R3"
      logoWidth="33px"
      right={
        <>
          <Dropdown
            closeOnSelectOption
            positionX="right"
            positionY="bottom"
            trigger={<IconCustom className="h-5" icon="Account" />}
          >
            {list.map((participant) => (
              <Option
                key={participant.id}
                value={participant.displayName}
                onClick={() => history.push(getParticipantPath(participant))}
              >
                {participant.displayName}
              </Option>
            ))}
          </Dropdown>
        </>
      }
      title="Cross Chain Swap Demo"
    />
  );
}

export default SiteHeader;
