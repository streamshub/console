"use client";

import { useEffect, useState } from "react";
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

  const setReconciliationPaused = (paused: boolean) => {
    setIsReconciliationPaused(paused);
  };

  useEffect(() => {
    const fetchReconciliationState = async () => {
      try {
        const cluster = await getKafkaCluster(kafkaId);
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
  }, [kafkaId]);

  return (
    <ReconciliationContext.Provider
      value={{ isReconciliationPaused, setReconciliationPaused }}
    >
      {children}
    </ReconciliationContext.Provider>
  );
}
