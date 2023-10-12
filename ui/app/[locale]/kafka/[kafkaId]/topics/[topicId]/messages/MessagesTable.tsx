"use client";
import { Message } from "@/api/messages";
import { useFilterParams } from "@/utils/useFilterParams";
import { useState } from "react";
import { KafkaMessageBrowser } from "./_components/KafkaMessageBrowser";

export function MessagesTable({
  messages,
  partitions,
  offsetMin,
  offsetMax,
  params,
}: {
  messages: Message[];
  partitions: number;
  offsetMin: number | undefined;
  offsetMax: number | undefined;
  params: {
    selectedOffset: number | undefined;
    partition: number | undefined;
    "filter[offset]": number | undefined;
    "filter[timestamp]": string | undefined;
    "filter[epoch]": number | undefined;
    limit: number;
  };
}) {
  const [currentParams, setParams] = useState(params);
  const updateUrl = useFilterParams(params);

  function setPartition(partition: number | undefined) {
    setParams((params) => ({ ...params, partition }));
  }

  function setOffset(offset: number | undefined) {
    setParams((params) => ({ ...params, "filter[offset]": offset }));
  }

  function setTimestamp() {}

  function setEpoch() {}

  function setLatest() {}

  function setLimit(limit: number) {
    setParams((params) => ({ ...params, limit }));
  }

  function setSelected(message: Message) {
    updateUrl({
      ...params,
      selectedOffset: message.attributes.offset,
    });
  }

  function deselectMessage() {
    updateUrl({
      ...params,
      selectedOffset: undefined,
    });
  }

  function refresh() {
    updateUrl(currentParams);
  }

  const selectedMessage = messages.find(
    (m) => m.attributes.offset === params.selectedOffset,
  );

  return (
    <KafkaMessageBrowser
      isFirstLoad={false}
      isNoData={false}
      isRefreshing={false}
      requiresSearch={JSON.stringify(params) !== JSON.stringify(currentParams)}
      selectedMessage={selectedMessage}
      lastUpdated={new Date()}
      messages={messages}
      offsetMin={offsetMin}
      offsetMax={offsetMax}
      partitions={partitions}
      partition={currentParams.partition}
      limit={currentParams.limit}
      filterOffset={currentParams["filter[offset]"]}
      filterEpoch={currentParams["filter[epoch]"]}
      filterTimestamp={currentParams["filter[timestamp]"]}
      setPartition={setPartition}
      setOffset={setOffset}
      setTimestamp={setTimestamp}
      setEpoch={setEpoch}
      setLatest={setLatest}
      setLimit={setLimit}
      refresh={refresh}
      selectMessage={setSelected}
      deselectMessage={deselectMessage}
    />
  );
}
