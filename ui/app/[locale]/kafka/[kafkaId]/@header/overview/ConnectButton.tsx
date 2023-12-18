"use client";

import { useOpenClusterConnectionPanel } from "@/app/[locale]/ClusterDrawerContext";
import { Button } from "@/libs/patternfly/react-core";

export function ConnectButton({ clusterId }: { clusterId: string }) {
  const open = useOpenClusterConnectionPanel();
  return (
    <Button onClick={() => open(clusterId)}>Cluster connection details</Button>
  );
}
