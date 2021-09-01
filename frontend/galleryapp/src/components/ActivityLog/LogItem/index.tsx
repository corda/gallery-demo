import styles from "./styles.module.scss";

function LogItem() {
  return (
    <div className={styles.main}>
      <span className={styles.colourCode} />
      <span className={styles.timestamp}>15:17:08.132263</span>
      <span className={styles.message}>Asset added to gallery network</span>
    </div>
  );
}

export default LogItem;
