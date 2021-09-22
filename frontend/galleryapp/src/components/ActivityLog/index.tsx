import styles from "./styles.module.scss";
import LogItem from "@Components/ActivityLog/LogItem";
import { Log } from "@Models";
import { IconButton } from "@r3/r3-tooling-design-system";
import { useState } from "react";

interface Props {
  title: string;
  inline?: boolean;
  logs: Log[];
}

function ActivityLog({ title, inline, logs }: Props) {
  const [expanded, setExpanded] = useState(false);
  const toggleIcon = expanded ? "ArrowCollapse" : "ArrowExpand";
  return (
    <>
      {expanded ? <div className={styles.overlay} onClick={() => setExpanded(false)} /> : null}
      <section
        className={`${styles.main} ${inline ? styles.small : ""} ${
          expanded ? styles.expanded : ""
        }`}
      >
        <header>
          <h6>{title}</h6>
          <IconButton
            dark
            icon={toggleIcon}
            size="small"
            variant="icon"
            onClick={() => setExpanded(!expanded)}
          />
        </header>
        <div className={styles.log}>
          {logs
            .sort((logA, logB) => (logA.timestamp > logB.timestamp ? -1 : 1))
            .map((log) => (
              <LogItem key={log.logRecordId} log={log} />
            ))}
        </div>
      </section>
    </>
  );
}

export default ActivityLog;
