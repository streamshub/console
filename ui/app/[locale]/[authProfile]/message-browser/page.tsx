import {
  KafkaMessageBrowser,
  KafkaMessageBrowserProps,
} from "@/components/messageBrowser/KafkaMessageBrowser";
import { NoDataEmptyState } from "@/components/messageBrowser/NoDataEmptyState";
import { SelectTopicEmptyState } from "@/components/messageBrowser/SelectTopicEmptyState";
import { getSession } from "@/utils/session";
import { revalidatePath } from "next/cache";

export default async function Principals() {
  const session = await getSession();

  const { topic } = session || {};
  const data = null; // TODO

  switch (true) {
    case topic === undefined:
      return <SelectTopicEmptyState />;
    case topic && data === null:
      return (
        <NoDataEmptyState
          onRefresh={() => revalidatePath("/message-browser")}
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
