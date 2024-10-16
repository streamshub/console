"use client";

import { useState } from "react";
import { ReconciliationContext } from "./ReconciliationContext";

export function ReconciliationProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [isReconciliationPaused, setIsReconciliationPaused] =
    useState<boolean>(false);

  const setReconciliationPaused = (paused: boolean) => {
    setIsReconciliationPaused(paused);
  };

  return (
    <ReconciliationContext.Provider
      value={{ isReconciliationPaused, setReconciliationPaused }}
    >
      {children}
    </ReconciliationContext.Provider>
  );
}
