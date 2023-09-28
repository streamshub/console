import { getResource } from "@/api/resources";
import { getTopicMessages } from "@/api/topics";
import {
  KafkaMessageBrowser,
  KafkaMessageBrowserProps,
} from "@/components/messageBrowser/KafkaMessageBrowser";
import { NoDataEmptyState } from "@/components/messageBrowser/NoDataEmptyState";
import { revalidateTag } from "next/cache";

export default async function Principals({
  params,
}: {
  params: { kafkaId: string; topic: string };
}) {
  const cluster = await getResource(params.kafkaId, "kafka");
  const data = await getTopicMessages(
    cluster.attributes.cluster!.id,
    params.topic,
  );
  switch (true) {
    case data === null:
      return (
        <NoDataEmptyState
          onRefresh={() => revalidateTag(`messages-${params.topic}`)}
        />
      );
    default:
      return <Table response={data} />;
  }
}

function Table({ response }: Pick<KafkaMessageBrowserProps, "response">) {
  return (
    <KafkaMessageBrowser
      isFirstLoad={false}
      isNoData={false}
      isRefreshing={false}
      requiresSearch={false}
      selectedMessage={undefined}
      lastUpdated={new Date()}
      response={response}
      partition={undefined}
      limit={10}
      filterOffset={undefined}
      filterEpoch={undefined}
      filterTimestamp={undefined}
      setPartition={setPartition}
      setOffset={setOffset}
      setTimestamp={setTimestamp}
      setEpoch={setEpoch}
      setLatest={setLatest}
      setLimit={setLimit}
      refresh={refresh}
      selectMessage={selectMessage}
      deselectMessage={deselectMessage}
    />
  );
}

async function setPartition() {
  "use server";
}
async function setOffset() {
  "use server";
}
async function setTimestamp() {
  "use server";
}
async function setEpoch() {
  "use server";
}
async function setLatest() {
  "use server";
}
async function setLimit() {
  "use server";
}
async function refresh() {
  "use server";
}
async function selectMessage() {
  "use server";
}
async function deselectMessage() {
  "use server";
}
