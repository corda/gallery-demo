import React, { createContext, FC, useState } from "react";
import { Balance, Participant, Token } from "@Models";
import useInterval from "@Hooks/useInterval";
import { getBalances } from "@Api";
import { isEqual } from "lodash";

interface TokensContextInterface {
  balances: Balance[];
  getTokensByUser: (user: Participant) => Token[];
}

const contextDefaultValues: TokensContextInterface = {
  balances: [],
  getTokensByUser: () => [],
};

export const TokensContext = createContext<TokensContextInterface>(contextDefaultValues);

export const TokensProvider: FC = ({ children }) => {
  const [balances, setBalances] = useState<Balance[]>(contextDefaultValues.balances);

  useInterval(async () => {
    const bs = await getBalances();
    if (bs && !isEqual(bs, balances)) setBalances(bs);
  }, 3000);

  function getTokensByUser(user: Participant): Token[] {
    if (!user.networkIds[0]) return [];

    const bs = balances.find((balance) => balance.x500 === user.x500);

    if (!bs) return [];

    return bs.partyBalances;
  }

  return (
    <TokensContext.Provider
      value={{
        balances: balances,
        getTokensByUser,
      }}
    >
      {children}
    </TokensContext.Provider>
  );
};
