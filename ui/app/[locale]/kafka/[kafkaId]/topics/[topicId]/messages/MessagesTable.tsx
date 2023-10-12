"use client";
import { Message } from "@/api/messages";
import { isSameMessage } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/utils";
import { useFilterParams } from "@/utils/useFilterParams";
import { useEffect, useState } from "react";
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
    partition: number | undefined;
    limit: number;
  };
}) {
  const [selected, setSelected] = useState<Message | undefined>();
  const updateUrl = useFilterParams(params);

  function setPartition(partition: number | undefined) {
    updateUrl("partition", partition?.toString());
  }

  function setOffset() {}

  function setTimestamp() {}

  function setEpoch() {}

  function setLatest() {}

  function setLimit(limit: number) {
    updateUrl("limit", limit.toString());
  }

  function refresh() {}

  useEffect(
    function checkIfSelectedMessageStillPresent() {
      if (selected && !messages.find((m) => isSameMessage(m, selected))) {
        setSelected(undefined);
      }
    },
    [messages, selected],
  );

  return (
    <KafkaMessageBrowser
      isFirstLoad={false}
      isNoData={false}
      isRefreshing={false}
      requiresSearch={false}
      selectedMessage={selected}
      lastUpdated={new Date()}
      messages={messages}
      offsetMin={offsetMin}
      offsetMax={offsetMax}
      partitions={partitions}
      partition={params.partition}
      limit={params.limit}
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
      selectMessage={setSelected}
      deselectMessage={() => setSelected(undefined)}
    />
  );
}
