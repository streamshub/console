"use client";

import { Dryrun, NewOffset } from "./Dryrun";
import { useRouter } from "@/i18n/routing";

export function ConnectedDryrunPage({
  groupId,
  offsetvalue,
  baseurl,
  cliCommand,
}: {
  groupId: string;
  offsetvalue: NewOffset[];
  baseurl: string;
  cliCommand: string;
}) {
  const router = useRouter();

  const onClickCloseDryrun = () => {
    router.push(baseurl);
  };

  return (
    <Dryrun
      consumerGroupName={groupId}
      newOffset={offsetvalue}
      onClickCloseDryrun={onClickCloseDryrun}
      cliCommand={cliCommand}
    />
  );
}
