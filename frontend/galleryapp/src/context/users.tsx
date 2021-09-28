import React, { createContext, FC, useCallback, useEffect, useState } from "react";
import { Participant } from "@Models";
import { getParticipants } from "@Api";
import { convertToKebabCase } from "@Utils";
import { uniqBy } from "lodash";
import config from "@Config";

interface UsersContextInterface {
  list: Participant[] | null;
  updateUsersList: () => void;
  getUser: (id: string) => Participant | null;
  networkColours: { [networkId: string]: string };
}

const contextDefaultValues: UsersContextInterface = {
  list: null,
  updateUsersList: () => {},
  getUser: () => null,
  networkColours: {},
};

export const UsersContext = createContext<UsersContextInterface>(contextDefaultValues);

export const UsersProvider: FC = ({ children }) => {
  const [list, setList] = useState<Participant[] | null>(contextDefaultValues.list);
  const [networkColours, setNetworkColours] = useState<{ [networkId: string]: string }>({});

  const fetchUsers = useCallback(async () => {
    if (!list) {
      const users = await getParticipants();

      if (users) {
        setList(users);
        setNetworkColours(getNetworkColours(users));
      }
    }
  }, [list]);

  const getUser = (displayName: string) => {
    if (!list) return null;

    const user = list.find((user) => convertToKebabCase(user.displayName) === displayName);

    if (!user) {
      return null;
    }

    return user;
  };

  function getNetworkColours(users: Participant[]) {
    const networkColours: { [k: string]: string } = {};

    uniqBy(
      users.reduce((previousValue: string[], currentValue) => {
        return previousValue.concat(
          currentValue.networkIds.map((networkIds) => networkIds.network)
        );
      }, []),
      (n) => n
    ).forEach((network, i) => {
      networkColours[network] = config.networkColours[i];
    });

    return networkColours;
  }

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  return (
    <UsersContext.Provider
      value={{
        list,
        updateUsersList: fetchUsers,
        getUser,
        networkColours,
      }}
    >
      {children}
    </UsersContext.Provider>
  );
};
