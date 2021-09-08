import styles from "./styles.module.scss";
import { Wallet } from "../../../models";
import ActivityLog from "@Components/ActivityLog";

interface Props {
  wallet: Wallet;
}

function WalletLog({ wallet }: Props) {
  return (
    <tr className={styles.main}>
      <td colSpan={6} className={styles.bidsRow}>
        <div className={styles.log}>
          <ActivityLog title="Wallet Audit Log" small={true} />
        </div>
      </td>
    </tr>
  );
}

export default WalletLog;
