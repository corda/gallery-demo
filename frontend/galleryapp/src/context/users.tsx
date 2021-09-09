import React, { createContext, FC, useCallback, useEffect, useState } from "react";
import { Participant } from "@Models";
import getParticipants from "@Api/getParticipants";
import { convertToKebabCase } from "@Helpers";

interface UsersContextInterface {
  list: Participant[] | null;
  updateUsersList: () => void;
  getUser: (id: string) => Participant | null;
}

const contextDefaultValues: UsersContextInterface = {
  list: null,
  updateUsersList: () => {},
  getUser: () => null,
};

export const UsersContext = createContext<UsersContextInterface>(contextDefaultValues);

export const UsersProvider: FC = ({ children }) => {
  const [list, setList] = useState<Participant[] | null>(contextDefaultValues.list);

  const fetchUsers = useCallback(async () => {
    if (!list) {
      const users = await getParticipants();
      setList(users);
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

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  return (
    <UsersContext.Provider
      value={{
        list,
        updateUsersList: fetchUsers,
        getUser,
      }}
    >
      {children}
    </UsersContext.Provider>
  );
};
