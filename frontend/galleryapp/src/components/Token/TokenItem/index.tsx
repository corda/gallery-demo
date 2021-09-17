import styles from "./styles.module.scss";
import { Token } from "@Models";
import { useContext, useState } from "react";
import { ReactComponent as Chevron } from "@Assets/chevronIcon.svg";
import ActivityLog from "@Components/ActivityLog";
import { LogsContext } from "@Context/logs";
import CurrencyBadge from "@Components/CurrencyBadge";

interface Props {
  token: Token;
  x500: string;
}

function TokenItem({ token, x500 }: Props) {
  const [isToggled, setToggled] = useState(false);
  const { getFilteredLogs } = useContext(LogsContext);
  const logs = getFilteredLogs(x500, token.currencyCode);

  return (
    <>
      <tr className={styles.main} onClick={() => setToggled(!isToggled)}>
        <td className={`${styles.chevron} ${isToggled ? styles.open : ""}`}>
          <Chevron />
        </td>
        <td>
          <CurrencyBadge currencyCode={token.currencyCode}>{token.currencyCode}</CurrencyBadge>
        </td>
        <td>{token.encumberedFunds}</td>
        <td>{token.availableFunds}</td>
      </tr>

      {isToggled && (
        <tr>
          <td colSpan={4} className={styles.logHolder}>
            <ActivityLog title="Token Audit Log" inline={true} logs={logs} />
          </td>
        </tr>
      )}
    </>
  );
}

export default TokenItem;
