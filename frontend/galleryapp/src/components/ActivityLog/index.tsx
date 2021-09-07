import styles from "./styles.module.scss";
import LogItem from "@Components/ActivityLog/LogItem";

interface Props {
  title: string;
  small?: boolean;
}

function ActivityLog({ title, small }: Props) {
  return (
    <section className={`${styles.main} ${small ? styles.small : ""}`}>
      <header>
        <h6>{title}</h6>
        <div>
          {/*<button>Minimize</button>*/}
          {/*<button>Maximize</button>*/}
        </div>
      </header>
      <div className={styles.log}>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
        <LogItem></LogItem>
      </div>
    </section>
  );
}

export default ActivityLog;
