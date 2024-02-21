"use client";

import { GetTopicMessagesReturn } from "@/api/messages/actions";
import { Message } from "@/api/messages/schema";
import { ConnectedRefreshSelector } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/ConnectedRefreshSelector";
import { RefreshInterval } from "@/components/RefreshSelector";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { useFilterParams } from "@/utils/useFilterParams";
import { AlertActionLink } from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, useTransition } from "react";
import { MessagesTable } from "./_components/MessagesTable";

export function ConnectedMessagesTable({
  messages: serverData,
  lastRefresh: serverRefresh,
  selectedMessage,
  partitions,
  params,
  onRefresh,
}: {
  messages: Message[];
  lastRefresh: Date;
  selectedMessage: Message | undefined;
  partitions: number;
  params: {
    selected: string | undefined;
    partition: number | undefined;
    query: string | undefined;
    "filter[offset]": number | undefined;
    "filter[timestamp]": string | undefined;
    "filter[epoch]": number | undefined;
    limit: number;
  };
  onRefresh: () => Promise<GetTopicMessagesReturn>;
}) {
  const [{ messages, ts, error }, setMessages] =
    useState<GetTopicMessagesReturn>({
      messages: serverData,
      ts: serverRefresh,
    });
  const [automaticRefresh, setAutomaticRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(1);
  const [isPending, startTransition] = useTransition();
  const updateUrl = useFilterParams(params);
  const router = useRouter();

  function setQuery(query: string | undefined) {
    startTransition(() => {
      updateUrl({
        ...params,
        query,
      });
    });
  }

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
    async function doRefresh(append = false) {
      const { messages: newMessages, ts, error } = await onRefresh();
      if (error) {
        setMessages({ messages, ts, error });
      } else if (append) {
        const messagesToAdd = newMessages.filter(
          (m) =>
            !messages.find(
              (m2) =>
                m2.attributes.offset === m.attributes.offset &&
                m2.attributes.partition === m.attributes.partition,
            ),
        );
        console.log({ messages, newMessages, messagesToAdd });
        startTransition(() => {
          setMessages({
            messages: Array.from(new Set([...messagesToAdd, ...messages])),
            ts,
          });
        });
      } else {
        startTransition(() => {
          setMessages({
            messages: newMessages,
            ts,
          });
        });
      }
    },
    [messages, onRefresh],
  );

  useEffect(() => {
    let t: ReturnType<typeof setTimeout> | undefined;

    async function tick() {
      if (automaticRefresh && t === undefined) {
        await doRefresh(true);
      }
      t = setTimeout(tick, refreshInterval * 1000);
    }

    void tick();
    return () => {
      clearTimeout(t);
      t = undefined;
    };
  }, [params, updateUrl, automaticRefresh, refreshInterval, doRefresh]);

  useEffect(() => {
    setMessages({ messages: serverData, ts: serverRefresh });
  }, [
    params.limit,
    params.partition,
    params["filter[epoch]"],
    params["filter[offset]"],
    params["filter[timestamp]"],
    params.query,
  ]);

  return (
    <>
      {error === "topic-not-found" && (
        <PageSection>
          <Alert
            variant="danger"
            title="Topic not found"
            ouiaId="topic-not-found"
            actionLinks={
              <AlertActionLink onClick={() => router.push("../")}>
                Go back to the list of topics
              </AlertActionLink>
            }
          >
            This topic was deleted, or you don&amp;t have the correct
            permissions to see it.
          </Alert>
        </PageSection>
      )}
      <MessagesTable
        isRefreshing={isPending}
        selectedMessage={selectedMessage}
        lastUpdated={new Date()}
        messages={messages}
        partitions={partitions}
        limit={params.limit}
        filterQuery={params.query}
        filterOffset={params["filter[offset]"]}
        filterEpoch={params["filter[epoch]"]}
        filterTimestamp={params["filter[timestamp]"]}
        filterPartition={params.partition}
        onQueryChange={setQuery}
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
        lastRefresh={ts}
        onRefresh={() => doRefresh()}
        onRefreshInterval={setRefreshInterval}
        onToggleLive={setAutomaticRefresh}
      />
    </>
  );
}
