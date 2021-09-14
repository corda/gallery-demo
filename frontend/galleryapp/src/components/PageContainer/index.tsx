import ActivityLog from "@Components/ActivityLog";
import { FC, useContext } from "react";
import { LogsContext } from "@Context/logs";

const PageContainer: FC = ({ children }) => {
  const { logs } = useContext(LogsContext);
  return (
    <>
      <div className="content-wrapper">{children}</div>
      <div className="footer-wrapper">
        <ActivityLog title="Global activity log" logs={logs} />
      </div>
    </>
  );
};

export default PageContainer;
