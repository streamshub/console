import { createContext, useContext } from "react";

export const ClusterDrawerContext = createContext<{
  open: (clusterId: string) => void;
  close: () => void;
  expanded: boolean;
  clusterId: string | undefined;
}>(null!);

export function useOpenClusterConnectionPanel() {
  const { open } = useContext(ClusterDrawerContext);
  return open;
}

export function useClusterDrawerContext() {
  return useContext(ClusterDrawerContext);
}
