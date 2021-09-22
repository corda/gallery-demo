import styles from "./styles.module.scss";
import { Log } from "@Models";
import config from "@Config";
import Locked from "@Assets/padlock.svg";
import Unlocked from "@Assets/unlocked.svg";
import LogDirectionArrow from "@Assets/logDirectionArrow.svg";

interface Props {
  log: Log;
}

function LogItem({ log }: Props) {
  const time = log.timestamp.split(" ")[3];
  return (
    <div className={styles.main}>
      <span
        className={styles.colourCode}
        style={{ background: config.networks[log.network].color }}
      />
      <span className={styles.timestamp}>{time}</span>
      <span className={styles.message}>{formatter(log.message)}</span>
    </div>
  );
}

function formatter(msg: string) {
  let newMsg = msg;
  newMsg = newMsg.replaceAll("[", `<span class="${styles.dataBlock}">`);
  newMsg = newMsg.replaceAll("]", "</span>");
  newMsg = newMsg.replaceAll(
    "<Locked>",
    `<img alt="icon" src="${Locked}" class="${styles.icon}"/>`
  );
  newMsg = newMsg.replaceAll(
    "<Unlocked>",
    `<img alt="icon" src="${Unlocked}" class="${styles.icon}"/>`
  );
  newMsg = newMsg.replaceAll("|", `<br/>`);
  newMsg = newMsg.replaceAll(
    "->",
    `<img alt="icon" src="${LogDirectionArrow}" class="${styles.arrow}"/>`
  );
  newMsg = newMsg.replaceAll(
    "<-",
    `<img alt="icon" src="${LogDirectionArrow}" class="${styles.arrow} ${styles.reverse}"/>`
  );

  return <div className={styles.formattedMessage} dangerouslySetInnerHTML={{ __html: newMsg }} />;
}

export default LogItem;
