import { TopNavBar } from "@r3/r3-tooling-design-system";
import React, { useContext } from "react";
import { useParams } from "react-router-dom";
import { UsersContext } from "@Context/users";
import { RouterParams } from "@Models";
import logo from "@Assets/logo.svg";
import UserDropdown from "@Components/SiteHeader/UserDropdown";
import FlowsDropdown from "@Components/SiteHeader/FlowsDropdown";

function SiteHeader() {
  const { list } = useContext(UsersContext);

  return (
    <TopNavBar
      logoAlt="R3"
      logoWidth="33px"
      logo={logo}
      center={
        <>
          <FlowsDropdown />
        </>
      }
      right={
        <>
          <UserDropdown currentUser={null} userList={list} />
        </>
      }
      title="Cross Chain Swap Demo"
    />
  );
}

export default SiteHeader;
