"use client";

import { Message } from "@/api/messages/schema";
import { ConnectedRefreshSelector } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedRefreshSelector";
import { RefreshInterval } from "@/components/RefreshSelector";
import { useFilterParams } from "@/utils/useFilterParams";
import { useCallback, useEffect, useState, useTransition } from "react";
import { MessagesTable } from "./_components/MessagesTable";

export function ConnectedMessagesTable({
  messages: initialData,
  lastRefresh: initialRefresh,
  selectedMessage,
  partitions,
  params,
  refresh,
}: {
  messages: Message[];
  lastRefresh: Date | undefined;
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
  refresh: () => Promise<{ messages: Message[]; ts: Date }>;
}) {
  const [{ messages, lastRefresh }, setMessages] = useState({
    messages: initialData,
    lastRefresh: initialRefresh,
  });
  const [automaticRefresh, setAutomaticRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(1);
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

  const doRefresh = useCallback(
    async function doRefresh() {
      const { messages, ts } = await refresh();
      startTransition(() => {
        setMessages({ messages, lastRefresh: ts });
      });
    },
    [refresh],
  );

  useEffect(() => {
    let t: ReturnType<typeof setTimeout>;

    async function tick() {
      if (automaticRefresh) {
        await doRefresh();
      }
      t = setTimeout(tick, refreshInterval * 1000);
    }

    void tick();
    return () => clearTimeout(t);
  }, [params, updateUrl, automaticRefresh, refreshInterval, doRefresh]);

  return (
    <>
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
        onPartitionChange={setPartition}
        onOffsetChange={setOffset}
        onTimestampChange={setTimestamp}
        onEpochChange={setEpoch}
        onLatest={setLatest}
        onLimitChange={setLimit}
        onSelectMessage={setSelected}
        onDeselectMessage={deselectMessage}
      />

      <ConnectedRefreshSelector
        isRefreshing={isPending}
        isLive={automaticRefresh}
        refreshInterval={refreshInterval}
        lastRefresh={lastRefresh}
        onRefresh={doRefresh}
        onRefreshInterval={setRefreshInterval}
        onToggleLive={setAutomaticRefresh}
      />
    </>
  );
}
