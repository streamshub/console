"use client";
import { Message } from "@/api/messages";
import { useFilterParams } from "@/utils/useFilterParams";
import { useTransition } from "react";
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
  const [isPending, startTransition] = useTransition();
  const updateUrl = useFilterParams(params);

  function setPartition(partition: number | undefined) {
    startTransition(() => {
      updateUrl({
        ...params,
        partition,
      });
    });
  }

  function setOffset(offset: number | undefined) {
    startTransition(() => {
      updateUrl({
        ...params,
        "filter[epoch]": undefined,
        "filter[timestamp]": undefined,
        "filter[offset]": offset,
      });
    });
  }

  function setTimestamp(value: string | undefined) {
    startTransition(() => {
      updateUrl({
        ...params,
        "filter[offset]": undefined,
        "filter[epoch]": undefined,
        "filter[timestamp]": value,
      });
    });
  }

  function setEpoch(value: number | undefined) {
    startTransition(() => {
      updateUrl({
        ...params,
        "filter[offset]": undefined,
        "filter[timestamp]": undefined,
        "filter[epoch]": value,
      });
    });
  }

  function setLatest() {
    startTransition(() => {
      updateUrl({
        ...params,
        "filter[offset]": undefined,
        "filter[timestamp]": undefined,
        "filter[epoch]": undefined,
      });
    });
  }

  function setLimit(limit: number) {
    startTransition(() => {
      updateUrl({
        ...params,
        limit,
      });
    });
  }

  function setSelected(message: Message) {
    startTransition(() => {
      updateUrl({
        ...params,
        selectedOffset: message.attributes.offset,
      });
    });
  }

  function deselectMessage() {
    startTransition(() => {
      updateUrl({
        ...params,
        selectedOffset: undefined,
      });
    });
  }

  function refresh() {
    startTransition(() => {
      updateUrl({ ...params, _ts: Date.now() });
    });
  }

  function onReset() {
    startTransition(() => {
      updateUrl({
        "filter[offset]": undefined,
        "filter[timestamp]": undefined,
        "filter[epoch]": undefined,
      });
    });
  }

  const selectedMessage = messages.find(
    (m) => m.attributes.offset === params.selectedOffset,
  );

  return (
    <KafkaMessageBrowser
      isFirstLoad={false}
      isNoData={false}
      isRefreshing={isPending}
      selectedMessage={selectedMessage}
      lastUpdated={new Date()}
      messages={messages}
      offsetMin={offsetMin}
      offsetMax={offsetMax}
      partitions={partitions}
      partition={params.partition}
      limit={params.limit}
      filterOffset={params["filter[offset]"]}
      filterEpoch={params["filter[epoch]"]}
      filterTimestamp={params["filter[timestamp]"]}
      setPartition={setPartition}
      setOffset={setOffset}
      setTimestamp={setTimestamp}
      setEpoch={setEpoch}
      setLatest={setLatest}
      setLimit={setLimit}
      refresh={refresh}
      selectMessage={setSelected}
      deselectMessage={deselectMessage}
      onReset={onReset}
    />
  );
}
