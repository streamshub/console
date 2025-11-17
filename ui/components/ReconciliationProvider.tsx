"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { ReconciliationContext } from "./ReconciliationContext";
import { getKafkaCluster } from "@/api/kafka/actions";

export function ReconciliationProvider({
  children,
  kafkaId,
}: {
  children: React.ReactNode;
  kafkaId: string;
}) {
  const [isReconciliationPaused, setIsReconciliationPaused] =
    useState<boolean>(false);

  const setReconciliationPaused = useCallback((paused: boolean) => {
    setIsReconciliationPaused(paused);
  }, []);

  useEffect(() => {
    if (!kafkaId) {
      return;
    }

    const fetchReconciliationState = async () => {
      try {
        const cluster = (await getKafkaCluster(kafkaId, { fields: "" }))?.payload;
        const reconciliationPaused =
          cluster?.meta?.reconciliationPaused ?? false;
        setReconciliationPaused(reconciliationPaused);
      } catch (e) {
        console.error("Error fetching reconciliation state", e);
      }
    };

    fetchReconciliationState();
    const intervalId = setInterval(fetchReconciliationState, 10000);
    return () => clearInterval(intervalId);
  }, [kafkaId, setReconciliationPaused]);

  const contextValue = useMemo(
    () => ({ isReconciliationPaused, setReconciliationPaused }),
    [isReconciliationPaused, setReconciliationPaused],
  );

  return (
    <ReconciliationContext.Provider value={contextValue}>
      {children}
    </ReconciliationContext.Provider>
  );
}
