import { getTopics } from "@/api/topics/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { Number } from "@/components/Number";
import {
  Label,
  Spinner,
  Split,
  SplitItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { OkIcon, WarningTriangleIcon } from "@/libs/patternfly/react-icons";
import { Suspense } from "react";

export default function TopicsHeader({ params }: { params: KafkaParams }) {
  return (
    <Suspense fallback={<Header />}>
      <ConnectedHeader params={params} />
    </Suspense>
  );
}

async function ConnectedHeader({ params }: { params: KafkaParams }) {
  const topics = await getTopics(params.kafkaId, {});
  return (
    <Header
      total={topics.meta.page.total}
      ok={topics.meta.page.total}
      warning={0}
    />
  );
}

function Header({
  total,
  ok,
  warning,
}: {
  total?: number;
  ok?: number;
  warning?: number;
}) {
  return (
    <AppHeader
      title={
        <Split hasGutter={true}>
          <SplitItem>Topics</SplitItem>
          <SplitItem>
            <Label
              icon={total === undefined ? <Spinner size={"sm"} /> : undefined}
            >
              {total !== undefined && <Number value={total} />}&nbsp;total
            </Label>
          </SplitItem>
          <SplitItem>
            <Tooltip content={"Number of topics in sync"}>
              <Label
                icon={ok === undefined ? <Spinner size={"sm"} /> : <OkIcon />}
                color={"cyan"}
              >
                {ok !== undefined && <Number value={ok} />}
              </Label>
            </Tooltip>
          </SplitItem>
          <SplitItem>
            <Tooltip content={"Number of topics under replicated"}>
              <Label
                icon={
                  warning === undefined ? (
                    <Spinner size={"sm"} />
                  ) : (
                    <WarningTriangleIcon />
                  )
                }
                color={"orange"}
              >
                {warning !== undefined && <Number value={warning} />}
              </Label>
            </Tooltip>
          </SplitItem>
        </Split>
      }
    />
  );
}
