import React, { createContext, FC, useState } from "react";
import { Log } from "@Models";
import useInterval from "@Hooks/useInterval";
import { getLogs } from "@Api";

interface LogsContextInterface {
  logs: Log[];
  getFilteredLogs: (x500: string | null, network: string) => Log[];
}

const contextDefaultValues: LogsContextInterface = {
  logs: [],
  getFilteredLogs: () => [],
};

export const LogsContext = createContext<LogsContextInterface>(contextDefaultValues);

function sortLogs(logA: Log, logB: Log) {
  if (logA.timestamp === logB.timestamp) return 0;
  return logA.timestamp < logB.timestamp ? 1 : -1;
}

export const LogsProvider: FC = ({ children }) => {
  const [logs, setLogs] = useState<Log[]>(contextDefaultValues.logs);

  useInterval(async () => {
    const ls = await getLogs(logs.length);
    if (ls && ls.length) {
      const newLogs = logs.concat(ls);
      setLogs(newLogs.sort(sortLogs));
    }
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
