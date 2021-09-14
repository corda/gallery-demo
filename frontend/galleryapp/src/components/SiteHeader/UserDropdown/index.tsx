import { Dropdown, IconCustom, Option } from "@r3/r3-tooling-design-system";
import { getParticipantPath } from "@Helpers";
import React from "react";
import { Participant } from "@Models";
import { useHistory } from "react-router-dom";
import styles from "./styles.module.scss";

interface Props {
  currentUser: Participant | null;
  userList: Participant[] | null;
}
function UserDropdown({ currentUser, userList }: Props) {
  const history = useHistory();

  return (
    <div>
      <Dropdown
        closeOnSelectOption
        positionX="right"
        positionY="bottom"
        trigger={
          <div className={styles.trigger}>
            {currentUser && <span className={styles.username}>{currentUser.displayName}</span>}
            <IconCustom className="h-5" icon="Account" />
          </div>
        }
      >
        {userList &&
          userList.map((participant) => (
            <Option
              key={participant.displayName}
              value={participant.displayName}
              onClick={() => history.push(getParticipantPath(participant))}
            >
              {participant.displayName}
            </Option>
          ))}
      </Dropdown>
    </div>
  );
}

export default UserDropdown;
