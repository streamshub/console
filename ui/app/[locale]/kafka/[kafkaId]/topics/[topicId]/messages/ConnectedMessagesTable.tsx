"use client";

import { Message } from "@/api/messages/schema";
import { RefreshInterval, RefreshSelector } from "@/components/RefreshSelector";
import { useFilterParams } from "@/utils/useFilterParams";
import { useEffect, useLayoutEffect, useState, useTransition } from "react";
import { createPortal } from "react-dom";
import { MessagesTable } from "./_components/MessagesTable";

export function ConnectedMessagesTable({
  messages,
  lastRefresh,
  selectedMessage,
  partitions,
  params,
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
}) {
  const [automaticRefresh, setAutomaticRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(1);
  const [isPending, startTransition] = useTransition();
  const [isAutorefreshing, startAutorefreshTransition] = useTransition();
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
    if (automaticRefresh) {
      interval = setInterval(async () => {
        if (!isAutorefreshing) {
          startAutorefreshTransition(() =>
            updateUrl({ ...params, _ts: Date.now() }),
          );
        }
      }, refreshInterval * 1000);
    }
    return () => clearInterval(interval);
  }, [params, updateUrl, automaticRefresh, refreshInterval, isAutorefreshing]);

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
        onRefresh={refresh}
        onRefreshInterval={setRefreshInterval}
        onToggleLive={setAutomaticRefresh}
      />
    </>
  );
}

function ConnectedRefreshSelector({
  isRefreshing,
  isLive,
  refreshInterval,
  lastRefresh,
  onRefresh,
  onRefreshInterval,
  onToggleLive,
}: {
  isRefreshing: boolean;
  isLive: boolean;
  refreshInterval: RefreshInterval;
  lastRefresh: Date | undefined;
  onRefresh: () => void;
  onRefreshInterval: (interval: RefreshInterval) => void;
  onToggleLive: (enable: boolean) => void;
}) {
  const [container, setContainer] = useState<Element | undefined>();

  function seekContainer() {
    const el = document.getElementById("topic-header-portal");
    if (el) {
      setContainer(el);
    } else {
      setTimeout(seekContainer, 100);
    }
  }

  useLayoutEffect(seekContainer, []); // we want to run this just once

  return container
    ? createPortal(
        <RefreshSelector
          isRefreshing={isRefreshing}
          isLive={isLive}
          refreshInterval={refreshInterval}
          lastRefresh={lastRefresh}
          onRefresh={onRefresh}
          onToggleLive={onToggleLive}
          onIntervalChange={onRefreshInterval}
        />,
        container,
      )
    : null;
}
