import React, { createContext, FC, useState } from "react";
import { Balance, Participant, Wallet } from "@Models";
import useInterval from "@Hooks/useInterval";
import getBalances from "@Api/getBalances";
import { isEqual } from "lodash";

interface WalletsContextInterface {
  balances: Balance[];
  getWalletsByUser: (user: Participant) => Wallet[];
}

const contextDefaultValues: WalletsContextInterface = {
  balances: [],
  getWalletsByUser: () => [],
};

export const WalletsContext = createContext<WalletsContextInterface>(contextDefaultValues);

export const WalletsProvider: FC = ({ children }) => {
  const [balances, setBalances] = useState<Balance[]>(contextDefaultValues.balances);

  useInterval(async () => {
    const bs = await getBalances();
    if (bs && !isEqual(bs, balances)) setBalances(bs);
  }, 2000);

  function getWalletsByUser(user: Participant): Wallet[] {
    if (!user.networkIds[0]) return [];

    const networkId = user.networkIds[0].x500;
    const bs = balances.find((balance) => balance.artworkParty === networkId);

    if (!bs) return [];

    return bs.balances;
  }

  return (
    <WalletsContext.Provider
      value={{
        balances: balances,
        getWalletsByUser,
      }}
    >
      {children}
    </WalletsContext.Provider>
  );
};
