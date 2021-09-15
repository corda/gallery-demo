import React, { createContext, FC, useState } from "react";
import { Log } from "../models";
import useInterval from "@Hooks/useInterval";
import {getLogs} from "@Api";
import { isEqual } from "lodash";

interface LogsContextInterface {
  logs: Log[];
  getFilteredLogs: (x500: string | null, network: string) => Log[];
}

const contextDefaultValues: LogsContextInterface = {
  logs: [],
  getFilteredLogs: () => [],
};

export const LogsContext = createContext<LogsContextInterface>(contextDefaultValues);

export const LogsProvider: FC = ({ children }) => {
  const [logs, setLogs] = useState<Log[]>(contextDefaultValues.logs);

  useInterval(async () => {
    const ls = await getLogs();
    if (ls && !isEqual(ls, logs)) setLogs(ls);
  }, 2000);

  function getFilteredLogs(x500: string | null, network: string): Log[] {
    return logs.filter((log) => {
      return (log.x500 === x500 || !x500) && log.network === network;
    });
  }

  return (
    <LogsContext.Provider
      value={{
        logs,
        getFilteredLogs,
      }}
    >
      {children}
    </LogsContext.Provider>
  );
};
