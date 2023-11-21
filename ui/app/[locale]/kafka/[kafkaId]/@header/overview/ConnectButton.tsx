"use client";

import { useHelp } from "@/components/Quickstarts/HelpContainer";
import { Button } from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";

export function ConnectButton() {
  const openHelp = useHelp();
  return (
    <Button
      variant={"link"}
      icon={<HelpIcon />}
      onClick={() => {
        openHelp("connect-to-cluster");
      }}
    >
      Cluster connection details
    </Button>
  );
}
