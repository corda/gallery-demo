import ActivityLog from "@Components/ActivityLog";
import React, { FC, useContext } from "react";
import { LogsContext } from "@Context/logs";
import SiteHeader from "@Components/SiteHeader";

const PageContainer: FC = ({ children }) => {
  const { logs } = useContext(LogsContext);

  return (
    <>
      <SiteHeader />
      <div className="content-wrapper">{children}</div>
      <div className="footer-wrapper">
        <ActivityLog title="Global activity log" logs={logs} />
      </div>
    </>
  );
};

export default PageContainer;
