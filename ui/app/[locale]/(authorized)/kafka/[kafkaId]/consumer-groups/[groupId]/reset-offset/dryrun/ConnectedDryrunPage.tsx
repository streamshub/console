"use client";

import { PageSection } from "@patternfly/react-core";
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

  const generateCliCommand = (): string => {
    let baseCommand = `$ kafka-consumer-groups --bootstrap-server \${bootstrap-Server} --group ${groupId} --reset-offsets`;
    offsetvalue.forEach((offset) => {
      baseCommand += ` --topic ${offset.topicName}`;
      if (offset.partition) {
        baseCommand += `:${offset.partition}`;
      }
      baseCommand += ` --to-offset ${offset.offset}`;
    });
    baseCommand += ` --dry-run`;
    return baseCommand;
  };

  return (
    <PageSection variant="light">
      <Dryrun
        consumerGroupName={groupId}
        newOffset={offsetvalue}
        onClickCloseDryrun={onClickCloseDryrun}
        cliCommand={cliCommand}
      />
    </PageSection>
  );
}
