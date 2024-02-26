"use client";

import {
  getTopicMessages,
  GetTopicMessagesReturn,
} from "@/api/messages/actions";
import { Message } from "@/api/messages/schema";
import { NoDataEmptyState } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/NoDataEmptyState";
import { useParseSearchParams } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { RefreshInterval } from "@/components/RefreshSelector";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { useFilterParams } from "@/utils/useFilterParams";
import { AlertActionLink } from "@patternfly/react-core";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, useTransition } from "react";
import {
  MessagesTable,
  MessagesTableSkeleton,
  SearchParams,
} from "./_components/MessagesTable";

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
  const searchParams = useParseSearchParams();
  const updateUrl = useFilterParams(searchParams);
  const router = useRouter();
  const { limit, partition, query, where, offset, timestamp, epoch, refresh } =
    searchParams;

  const [{ messages, ts, error }, setMessages] =
    useState<GetTopicMessagesReturn>({
      messages: undefined,
      ts: undefined,
    });
  const [automaticRefresh, setAutomaticRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(1);
  const [isPending, startTransition] = useTransition();

  const isFiltered = partition || epoch || offset || timestamp || query;

  function onSearch({ query, from, until, partition }: SearchParams) {
    setMessages({ messages: undefined, ts: undefined, error: undefined });
    startTransition(() => {
      const newQuery = {
        ...searchParams,
        query: query?.value,
        where: query?.where,
        partition,
        "filter[offset]": from.type === "offset" ? from.value : "",
        "filter[timestamp]": from.type === "timestamp" ? from.value : "",
        "filter[epoch]": from.type === "epoch" ? from.value : "",
        limit: until.type === "limit" ? until.value : "",
        _: Date.now(),
      };
      updateUrl(newQuery);
      setAutomaticRefresh(until.type === "live");
    });
  }

  function setSelected(message: Message) {
    startTransition(() => {
      updateUrl({
        ...searchParams,
        selected: `${message.attributes.partition}:${message.attributes.offset}`,
      });
    });
  }

  function deselectMessage() {
    startTransition(() => {
      updateUrl({
        ...searchParams,
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
      timestamp,
      append = false,
    }: {
      limit?: number;
      offset?: number;
      partition?: number;
      query?: string;
      timestamp?: string;
      append?: boolean;
    }) {
      const filter = offset
        ? { type: "offset" as const, value: offset }
        : timestamp
          ? { type: "timestamp" as const, value: timestamp }
          : undefined;

      const {
        messages: newMessages = [],
        ts,
        error,
      } = await getTopicMessages(kafkaId, topicId, {
        pageSize: limit ?? 50,
        query,
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
      if (automaticRefresh && t === undefined) {
        await fetchMessages({
          limit,
          offset,
          partition,
          query,
          timestamp,
          append: true,
        });
      }
      t = setTimeout(tick, refreshInterval * 1000);
    }

    void tick();
    return () => {
      clearTimeout(t);
      t = undefined;
    };
  }, [
    automaticRefresh,
    fetchMessages,
    limit,
    offset,
    partition,
    query,
    where,
    refreshInterval,
    timestamp,
  ]);

  useEffect(() => {
    void fetchMessages({
      limit,
      offset,
      partition,
      query,
      timestamp,
    });
  }, [fetchMessages, limit, offset, partition, query, timestamp, refresh]);

  switch (true) {
    case messages === undefined:
      return (
        <MessagesTableSkeleton
          filterLimit={limit}
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
          isRefreshing={isPending}
          selectedMessage={selectedMessage}
          lastUpdated={ts}
          messages={messages}
          partitions={partitions}
          filterLimit={limit}
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
