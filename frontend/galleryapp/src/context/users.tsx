import React, { createContext, FC, useEffect, useState } from "react";
import { Participant } from "../models";
import getUsers from "@Api/getUsers";

interface UsersContextInterface {
  list: Participant[];
  loading: boolean;
  updateUsersList: () => void;
}

const contextDefaultValues: UsersContextInterface = {
  list: [],
  loading: false,
  updateUsersList: () => {},
};

export const UsersContext = createContext<UsersContextInterface>(contextDefaultValues);

export const UsersProvider: FC = ({ children }) => {
  const [list, setList] = useState<Participant[]>(contextDefaultValues.list);
  const [loading, setLoading] = useState(false);

  const fetchUsers = async () => {
    if (!loading) {
      setLoading(true);
      const users = await getUsers();
      setList(users);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers()
  }, []);

  return (
    <UsersContext.Provider
      value={{
        list,
        loading,
        updateUsersList: fetchUsers,
      }}
    >
      {children}
    </UsersContext.Provider>
  );
};

//
// const UsersContext = React.createContext([]);
// function useUsers() {
//   const context = React.useContext(UsersContext);
//   if (!context) {
//     throw new Error(`useCount must be used within a CountProvider`);
//   }
//   return context;
// }
//
// function UsersProvider(props: React.FunctionComponentElement<any>) {
//   const [users, setUsers] = React.useState([]);
//   const value = React.useMemo(() => [users, setUsers], [users]);
//
//   return <UsersContext.Provider value={value} {...props} />;
// }
// export { UsersProvider, useUsers };

// const UsersContext = React.createContext<Participant[]>([])
//
// function useUsers() {
//     const context = React.useContext(UsersContext)
//     if (!context) {
//         throw new Error(`useCount must be used within a CountProvider`)
//     }
//     return context
// }
//
// function UsersProvider(props: React.PropsWithChildren<{}>) {
//     const [users, setUsers] = React.useState<UsersContextInterface>({list:[]})
//     const value = React.useMemo(() => [users, setUsers], [users])
//
//     return <UsersContext.Provider value={value} {...props} />
// }
//
// export {UsersProvider, useUsers}

// interface ParticipantsContextInterface {
//     participants: Participant[]
// }
//
// const ParticipantsContext = React.createContext<ParticipantsContextInterface | null>(null);
//
// export const User = () => (
//     <ParticipantsContext.Provider value={{participants: mockUsers}}/>
// );
//
// export default User
