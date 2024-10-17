import { createContext, useContext } from "react";

export const ReconciliationContext = createContext<{
  isReconciliationPaused: boolean;
  setReconciliationPaused: (paused: boolean) => void;
}>(null!);

export function useReconciliationContext() {
  return useContext(ReconciliationContext);
}
