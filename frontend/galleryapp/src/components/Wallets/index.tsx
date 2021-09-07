import styles from "./styles.module.scss";
import { Wallet } from "../../models";
import WalletItem from "@Components/Wallets/WalletItem";

interface Props {
  wallets: Wallet[];
}

function Wallets({ wallets }: Props) {
  return (
    <section className={styles.main}>
      <h2>Wallets</h2>
      <table>
        <thead>
          <tr>
            <th />
            <th>Wallet Id</th>
            <th>Asset Type</th>
            <th>Total</th>
          </tr>
        </thead>
        <tbody>
          {wallets.map((wallet) => (
            <WalletItem key={wallet.id} wallet={wallet} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default Wallets;
