import { TopNavBar } from "@r3/r3-tooling-design-system";
import styles from "./styles.module.scss";
import React, { useContext } from "react";
import { UsersContext } from "@Context/users";
import logo from "@Assets/logo.svg";
import UserDropdown from "@Components/SiteHeader/UserDropdown";
import FlowsDropdown from "@Components/SiteHeader/FlowsDropdown";
import { useParams } from "react-router-dom";
import { RouterParams } from "@Models";
import SettingsDropdown from "@Components/SiteHeader/SettingsDropdown";

function SiteHeader() {
  const { list, getUser } = useContext(UsersContext);
  const { id } = useParams<RouterParams>();
  const user = getUser(id);

  return (
    <header>
      <TopNavBar logoAlt="R3" logoWidth="33px" logo={logo} title="Cross Chain Swap Demo" />
      <div className={styles.dropdowns}>
        <FlowsDropdown />
        <UserDropdown currentUser={user} userList={list} />
        <SettingsDropdown />
      </div>
    </header>
  );
}

export default SiteHeader;
