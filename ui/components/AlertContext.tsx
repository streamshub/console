import { createContext, useContext } from "react";
import { GroupToastAlertProps } from "./AlertToastGroup";

type AlertContextType = {
  addAlert: (alert: Omit<GroupToastAlertProps, "id">) => void;
  removeAlert: (id: string) => void;
};

export const AlertContext = createContext<AlertContextType | undefined>(
  undefined,
);

export function useAlert() {
  const context = useContext(AlertContext);
  if (!context) {
    throw new Error("useAlert must be used within an AlertProvider");
  }
  return context;
}
