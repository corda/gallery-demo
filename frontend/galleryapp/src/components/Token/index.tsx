import styles from "./styles.module.scss";
import TokenItem from "@Components/Token/TokenItem";
import { Participant } from "@Models";
import { useContext } from "react";
import { TokensContext } from "@Context/tokens";

interface Props {
  user: Participant;
}

function Tokens({ user }: Props) {
  const { getTokensByUser } = useContext(TokensContext);
  const tokens = getTokensByUser(user);

  return (
    <section className={styles.main}>
      <h3>Balances</h3>
      <table>
        <thead>
          <tr>
            <th />
            <th>Token Type</th>
            <th>Encumbered</th>
            <th>Total</th>
          </tr>
        </thead>
        <tbody>
          {tokens.map((token) => (
            <TokenItem key={token.currencyCode} token={token} x500={user.x500} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default Tokens;
