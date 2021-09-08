import styles from "./styles.module.scss";
import LogItem from "@Components/ActivityLog/LogItem";
import { Log } from "@Models";

interface Props {
  title: string;
  inline?: boolean;
  logs: Log[];
}

function ActivityLog({ title, inline, logs }: Props) {
  return (
    <section className={`${styles.main} ${inline ? styles.small : ""}`}>
      <header>
        <h6>{title}</h6>
        <div>
          {/*<button>Minimize</button>*/}
          {/*<button>Maximize</button>*/}
        </div>
      </header>
      <div className={styles.log}>
        {logs
          .sort((logA, logB) => (logA.timestamp > logB.timestamp ? -1 : 1))
          .map((log) => (
            <LogItem key={log.logRecordId} log={log} />
          ))}
      </div>
    </section>
  );
}

export default ActivityLog;
