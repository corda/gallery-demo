import styles from "./styles.module.scss";
import { Wallet } from "@Models";
import { useContext, useState } from "react";
import { ReactComponent as Chevron } from "@Assets/chevronIcon.svg";
import ActivityLog from "@Components/ActivityLog";
import { LogsContext } from "@Context/logs";
import CurrencyBadge from "@Components/CurrencyBadge";

interface Props {
  wallet: Wallet;
  x500: string;
}

function WalletItem({ wallet, x500 }: Props) {
  const [isToggled, setToggled] = useState(false);
  const { getFilteredLogs } = useContext(LogsContext);
  const logs = getFilteredLogs(x500, wallet.currencyCode);

  return (
    <>
      <tr className={styles.main} onClick={() => setToggled(!isToggled)}>
        <td className={`${styles.chevron} ${isToggled ? styles.open : ""}`}>
          <Chevron />
        </td>
        <td>
          <CurrencyBadge currencyCode={wallet.currencyCode}>{wallet.currencyCode}</CurrencyBadge>
        </td>
        <td>{wallet.encumberedFunds}</td>
        <td>{wallet.availableFunds}</td>
      </tr>

      {isToggled && (
        <td colSpan={4} className={styles.logHolder}>
          <ActivityLog title="Wallet Audit Log" inline={true} logs={logs} />
        </td>
      )}
    </>
  );
}

export default WalletItem;
