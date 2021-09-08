import styles from "./styles.module.scss";
import { Wallet } from "../../../models";
import { useState } from "react";
import { ReactComponent as Chevron } from "@Assets/chevronIcon.svg";
import WalletLog from "@Components/Wallets/WalletLog";

interface Props {
  wallet: Wallet;
}

function WalletItem({ wallet }: Props) {
  const [isToggled, setToggled] = useState(false);

  return (
    <>
      <tr className={styles.main} onClick={() => setToggled(!isToggled)}>
        <td className={`${styles.chevron} ${isToggled ? styles.open : ""}`}>
          <Chevron />
        </td>
        <td>{wallet.id}</td>
        <td>{wallet.symbol}</td>
        <td>{wallet.availableFunds}</td>
      </tr>

      {isToggled && <WalletLog wallet={wallet} />}
    </>
  );
}

export default WalletItem;
