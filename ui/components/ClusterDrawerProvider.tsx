"use client";
import { PropsWithChildren, useState } from "react";
import { ClusterDrawerContext } from "./ClusterDrawerContext";

export function ClusterDrawerProvider({ children }: PropsWithChildren) {
  const [expanded, setExpanded] = useState(false);
  const [clusterId, setClusterId] = useState<string | undefined>();
  const open = (clusterId: string) => {
    setClusterId(clusterId);
    setExpanded(true);
  };
  const close = () => setExpanded(false);

  return (
    <ClusterDrawerContext.Provider value={{ open, close, expanded, clusterId }}>
      {children}
    </ClusterDrawerContext.Provider>
  );
}
