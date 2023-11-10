"use client";

import { Message } from "@/api/messages/schema";
import { RefreshInterval } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/RefreshSelector";
import { useFilterParams } from "@/utils/useFilterParams";
import { useEffect, useState, useTransition } from "react";
import { MessagesTable } from "./_components/MessagesTable";

export function ConnectedMessagesTable({
  messages,
  selectedMessage,
  partitions,
  params,
}: {
  messages: Message[];
  selectedMessage: Message | undefined;
  partitions: number;
  params: {
    selected: string | undefined;
    partition: number | undefined;
    "filter[offset]": number | undefined;
    "filter[timestamp]": string | undefined;
    "filter[epoch]": number | undefined;
    limit: number;
  };
}) {
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>();
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
        selected: `${message.attributes.partition}:${message.attributes.offset}`,
      });
    });
  }

  function deselectMessage() {
    startTransition(() => {
      updateUrl({
        ...params,
        selected: undefined,
      });
    });
  }

  function refresh() {
    startTransition(() => {
      updateUrl({ ...params, _ts: Date.now() });
    });
  }

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (refreshInterval) {
      interval = setInterval(async () => {
        updateUrl({ ...params, _ts: Date.now() });
      }, refreshInterval * 1000);
    }
    return () => clearInterval(interval);
  }, [params, updateUrl, refreshInterval]);

  return (
    <MessagesTable
      isRefreshing={isPending}
      selectedMessage={selectedMessage}
      lastUpdated={new Date()}
      messages={messages}
      partitions={partitions}
      partition={params.partition}
      limit={params.limit}
      filterOffset={params["filter[offset]"]}
      filterEpoch={params["filter[epoch]"]}
      filterTimestamp={params["filter[timestamp]"]}
      refreshInterval={refreshInterval}
      onPartitionChange={setPartition}
      onOffsetChange={setOffset}
      onTimestampChange={setTimestamp}
      onEpochChange={setEpoch}
      onLatest={setLatest}
      onLimitChange={setLimit}
      onRefresh={refresh}
      onSelectMessage={setSelected}
      onDeselectMessage={deselectMessage}
      onRefreshInterval={setRefreshInterval}
    />
  );
}
