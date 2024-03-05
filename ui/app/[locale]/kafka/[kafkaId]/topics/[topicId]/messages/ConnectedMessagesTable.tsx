"use client";

import {
  getTopicMessages,
  GetTopicMessagesReturn,
} from "@/api/messages/actions";
import { Message } from "@/api/messages/schema";
import { MessagesTable } from "@/components/MessagesTable/MessagesTable";
import { MessagesTableSkeleton } from "@/components/MessagesTable/MessagesTableSkeleton";
import { NoDataEmptyState } from "@/components/MessagesTable/NoDataEmptyState";
import { SearchParams } from "@/components/MessagesTable/types";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { useFilterParams } from "@/utils/useFilterParams";
import { AlertActionLink } from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, useTransition } from "react";
import { useParseSearchParams } from "./parseSearchParams";

export function ConnectedMessagesTable({
  kafkaId,
  topicId,
  selectedMessage,
  partitions,
}: {
  kafkaId: string;
  topicId: string;
  selectedMessage: Message | undefined;
  partitions: number;
}) {
  const [params, sp] = useParseSearchParams();
  const updateUrl = useFilterParams(sp);
  const router = useRouter();
  const { limit, live, partition, query, where, offset, timestamp, epoch, _ } =
    params;

  const [{ messages, ts, error }, setMessages] =
    useState<GetTopicMessagesReturn>({
      messages: undefined,
      ts: undefined,
    });
  const [isPending, startTransition] = useTransition();

  const isFiltered = partition || epoch || offset || timestamp || query;

  function onSearch({ query, from, until, partition }: SearchParams) {
    setMessages({ messages: undefined, ts: undefined, error: undefined });
    startTransition(() => {
      const newQuery = {
        query: query?.value,
        where: query?.where,
        partition,
        offset: from.type === "offset" ? from.value : "",
        timestamp: from.type === "timestamp" ? from.value : "",
        epoch: from.type === "epoch" ? from.value : "",
        limit: until.type === "limit" ? until.value : "",
        live: until.type === "live",
        _: Date.now(),
      };
      updateUrl(newQuery);
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

  const fetchMessages = useCallback(
    async function fetchMessages({
      limit,
      offset,
      partition,
      query,
      where,
      timestamp,
      epoch,
      append = false,
    }: {
      limit?: number;
      offset?: number;
      partition?: number;
      query?: string;
      where?: "key" | "headers" | "value" | `jq:${string}`;
      timestamp?: string;
      epoch?: number;
      append?: boolean;
    }) {
      const filter = (() => {
        if (offset) return { type: "offset" as const, value: offset };
        if (timestamp) return { type: "timestamp" as const, value: timestamp };
        if (epoch) return { type: "epoch" as const, value: epoch };
        return undefined;
      })();

      const {
        messages: newMessages = [],
        ts,
        error,
      } = await getTopicMessages(kafkaId, topicId, {
        pageSize: limit ?? 50,
        query,
        where,
        partition,
        filter,
        maxValueLength: 150,
      });
      if (error) {
        setMessages({ messages: newMessages, ts, error });
      } else if (append) {
        startTransition(() => {
          setMessages(({ messages = [] }) => {
            const messagesToAdd = newMessages.filter(
              (m) =>
                !messages.find(
                  (m2) =>
                    m2.attributes.offset === m.attributes.offset &&
                    m2.attributes.partition === m.attributes.partition,
                ),
            );

            return {
              messages: Array.from(new Set([...messagesToAdd, ...messages])),
              ts,
            };
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
    [kafkaId, topicId],
  );

  useEffect(() => {
    let t: ReturnType<typeof setTimeout> | undefined;

    async function tick() {
      if (live && messages) {
        const latestTs = messages[0]?.attributes.timestamp;
        await fetchMessages({
          limit: 50,
          partition,
          query,
          where,
          timestamp: latestTs,
          append: true,
        });
      }
      t = setTimeout(tick, 1000);
    }

    t = setTimeout(tick, 1000);
    return () => {
      clearTimeout(t);
      t = undefined;
    };
  }, [
    live,
    fetchMessages,
    limit,
    offset,
    partition,
    query,
    where,
    timestamp,
    epoch,
    messages,
  ]);

  useEffect(() => {
    void fetchMessages({
      limit,
      offset,
      partition,
      query,
      where,
      timestamp,
      epoch,
    });
  }, [
    fetchMessages,
    limit,
    offset,
    partition,
    query,
    timestamp,
    _,
    where,
    epoch,
  ]);

  switch (true) {
    case messages === undefined:
      return (
        <MessagesTableSkeleton
          filterLimit={limit}
          filterLive={live}
          filterTimestamp={timestamp}
          filterPartition={partition}
          filterOffset={offset}
          filterEpoch={epoch}
          filterQuery={query}
          filterWhere={where}
        />
      );
    case !isFiltered && messages?.length === 0:
      return <NoDataEmptyState />;
    case error === "topic-not-found":
      return (
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
            This topic was deleted, or you don&apos;t have the correct
            permissions to see it.
          </Alert>
        </PageSection>
      );
    default:
      return (
        <MessagesTable
          selectedMessage={selectedMessage}
          lastUpdated={ts}
          messages={messages}
          partitions={partitions}
          filterLimit={limit}
          filterLive={live}
          filterQuery={query}
          filterWhere={where}
          filterOffset={offset}
          filterEpoch={epoch}
          filterTimestamp={timestamp}
          filterPartition={partition}
          onSearch={onSearch}
          onSelectMessage={setSelected}
          onDeselectMessage={deselectMessage}
        />
      );

    /*
    <ConnectedRefreshSelector
      isRefreshing={isPending}
      isLive={automaticRefresh}
      refreshInterval={refreshInterval}
      lastRefresh={ts}
      onRefresh={() => doRefresh()}
      onRefreshInterval={setRefreshInterval}
      onToggleLive={setAutomaticRefresh}
    />
*/
  }
}
