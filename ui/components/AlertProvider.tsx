"use client";

import { useState } from "react";
import { AlertToastGroup, GroupToastAlertProps } from "./AlertToastGroup";
import { AlertContext } from "./AlertContext";

export function AlertProvider({ children }: { children: React.ReactNode }) {
  const [alerts, setAlerts] = useState<GroupToastAlertProps[]>([]);

  const addAlert = (
    alert: Omit<GroupToastAlertProps, "id">,
    timeout: number = 8000,
  ) => {
    const id = new Date().toISOString();
    setAlerts((prevAlerts) => [...prevAlerts, { ...alert, id }]);
    setTimeout(() => {
      removeAlert(id);
    }, timeout);
  };

  const removeAlert = (id: string) => {
    setAlerts((prevAlerts) => prevAlerts.filter((alert) => alert.id !== id));
  };

  return (
    <AlertContext.Provider value={{ addAlert, removeAlert }}>
      <AlertToastGroup
        toastAlerts={alerts}
        onClickCloseAlert={(id) => removeAlert(id)}
      />
      {children}
    </AlertContext.Provider>
  );
}
