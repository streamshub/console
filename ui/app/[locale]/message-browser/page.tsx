import { KafkaMessageBrowser } from "@/app/[locale]/message-browser/_components/KafkaMessageBrowser";

export default async function Principals() {
  return (
    <KafkaMessageBrowser
      isFirstLoad={false}
      isNoData={false}
      isRefreshing={false}
      requiresSearch={false}
      selectedMessage={undefined}
      lastUpdated={new Date()}
      response={{
        messages: [],
        filter: {
          timestamp: undefined,
          limit: undefined,
          partition: undefined,
          epoch: undefined,
          offset: undefined,
        },
        lastUpdated: new Date(),
        offsetMax: 100,
        offsetMin: 0,
        partitions: 3,
      }}
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
