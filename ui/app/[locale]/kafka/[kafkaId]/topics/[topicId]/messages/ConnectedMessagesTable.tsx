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
import { useCallback, useEffect, useState } from "react";
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
  const {
    limit,
    partition,
    query,
    where,
    offset,
    timestamp,
    epoch,
    _,
    selectedOffset,
    selectedPartition,
  } = params;

  const [{ messages, ts, error }, setMessages] =
    useState<GetTopicMessagesReturn>({
      messages: undefined,
      ts: undefined,
    });

  function onSearch({ query, from, limit, partition }: SearchParams) {
    setMessages({ messages: undefined, ts: undefined, error: undefined });
    const newQuery = {
      query: query?.value,
      where: query?.where,
      partition,
      offset: from.type === "offset" ? from.value : "",
      timestamp: from.type === "timestamp" ? from.value : "",
      epoch: from.type === "epoch" ? from.value : "",
      retrieve: limit,
      _: Date.now(),
    };
    updateUrl(newQuery);
  }

  function setSelected(message: Message) {
    updateUrl({
      ...params,
      selected: `${message.attributes.partition}:${message.attributes.offset}`,
    });
  }

  function deselectMessage() {
    updateUrl({
      ...params,
      selected: undefined,
    });
  }

  function onReset() {
    onSearch({
      query: undefined,
      from: {
        type: "latest",
      },
      limit: 50,
      partition: undefined,
    });
  }

  const fetchMessages = useCallback(
    async function fetchMessages() {
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
        pageSize: limit === "continuously" ? 50 : limit ?? 50,
        query,
        where,
        partition,
        filter,
        maxValueLength: 150,
      });
      if (error) {
        setMessages({ messages: newMessages, ts, error });
      } else {
        setMessages({
          messages: newMessages,
          ts,
        });
      }
    },
    [
      epoch,
      kafkaId,
      limit,
      offset,
      partition,
      query,
      timestamp,
      topicId,
      where,
    ],
  );

  const appendMessages = useCallback(async () => {
    const previousTs = messages
      ? messages[0]?.attributes.timestamp
      : new Date().toISOString();

    const {
      messages: newMessages = [],
      ts,
      error,
    } = await getTopicMessages(kafkaId, topicId, {
      pageSize: 10,
      query,
      where,
      partition,
      filter: {
        type: "timestamp",
        value: previousTs,
      },
      maxValueLength: 150,
    });
    if (error) {
      setMessages({ messages: newMessages, ts, error });
    } else {
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
          messages: Array.from(new Set([...messagesToAdd, ...messages])).slice(
            0,
            100,
          ),
          ts,
        };
      });
    }
  }, [kafkaId, messages, partition, query, topicId, where]);

  useEffect(() => {
    let t: ReturnType<typeof setTimeout> | undefined;

    async function tick() {
      if (limit === "continuously" && t === undefined) {
        await appendMessages();
        t = setTimeout(tick, 5000);
      }
    }

    if (limit === "continuously") {
      void tick();
    }

    return () => {
      clearTimeout(t);
      t = undefined;
    };
  }, [appendMessages, limit]);

  useEffect(() => {
    void fetchMessages();
  }, [
    fetchMessages,
    _, // when clicking search multiple times, the search parameters remain the same but a timestamp is added to _. We listen for changes to _ to know we have to trigger a new fetch
  ]);

  const isFiltered = partition || epoch || offset || timestamp || query;

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
    case !isFiltered && messages && messages?.length === 0:
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
          filterQuery={query}
          filterWhere={where}
          filterOffset={offset}
          filterEpoch={epoch}
          filterTimestamp={timestamp}
          filterPartition={partition}
          onSearch={onSearch}
          onSelectMessage={setSelected}
          onDeselectMessage={deselectMessage}
          onReset={onReset}
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
