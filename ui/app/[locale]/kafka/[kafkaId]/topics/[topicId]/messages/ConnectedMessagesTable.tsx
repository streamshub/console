"use client";

import {
  getTopicMessages,
  GetTopicMessagesReturn,
} from "@/api/messages/actions";
import { Message } from "@/api/messages/schema";
import { AlertContinuousMode } from "@/components/MessagesTable/AlertContinuousMode";
import { AlertTopicGone } from "@/components/MessagesTable/AlertTopicGone";
import { MessagesTable } from "@/components/MessagesTable/MessagesTable";
import { MessagesTableSkeleton } from "@/components/MessagesTable/MessagesTableSkeleton";
import { NoDataEmptyState } from "@/components/MessagesTable/NoDataEmptyState";
import { SearchParams } from "@/components/MessagesTable/types";
import { PageSection } from "@/libs/patternfly/react-core";
import { useFilterParams } from "@/utils/useFilterParams";
import { useRouter } from "next/navigation";
import {
  startTransition,
  useCallback,
  useEffect,
  useOptimistic,
  useRef,
  useState,
} from "react";
import { useParseSearchParams } from "./parseSearchParams";

export function ConnectedMessagesTable({
  kafkaId,
  topicId,
  topicName,
  selectedMessage: serverSelectedMessage,
  partitions,
}: {
  kafkaId: string;
  topicId: string;
  topicName: string;
  selectedMessage: Message | undefined;
  partitions: number;
}) {
  const [params, sp] = useParseSearchParams();
  const updateUrl = useFilterParams(sp);
  const router = useRouter();
  const { limit, partition, query, where, offset, timestamp, epoch, _ } =
    params;
  const [selectedMessage, setOptimisticSelectedMessage] = useOptimistic<
    Message | undefined
  >(serverSelectedMessage);

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
    startTransition(() => setOptimisticSelectedMessage(message));
    updateUrl({
      ...params,
      selected: `${message.attributes.partition}:${message.attributes.offset}`,
    });
  }

  function deselectMessage() {
    startTransition(() => setOptimisticSelectedMessage(undefined));
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

  useEffect(() => {
    void fetchMessages();
  }, [
    fetchMessages,
    _, // when clicking search multiple times, the search parameters remain the same but a timestamp is added to _. We listen for changes to _ to know we have to trigger a new fetch
  ]);

  const onUpdates = useCallback((newMessages: Message[], ts: Date) => {
    startTransition(() =>
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
      }),
    );
  }, []);

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
          <AlertTopicGone onClick={() => router.push("../")} />
        </PageSection>
      );
    default:
      return (
        <>
          <MessagesTable
            topicName={topicName}
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
          >
            {limit === "continuously" && (
              <Refresher
                topicId={topicId}
                kafkaId={kafkaId}
                query={query}
                where={where}
                partition={partition}
                onUpdates={onUpdates}
              />
            )}
          </MessagesTable>
        </>
      );
  }
}

function Refresher({
  kafkaId,
  topicId,
  query,
  where,
  partition,
  onUpdates,
}: {
  kafkaId: string;
  topicId: string;
  query?: string;
  where: any;
  partition?: number;
  onUpdates: (messages: Message[], ts: Date) => void;
}) {
  const previousTs = useRef<string>(new Date().toISOString());
  const isFetching = useRef(false);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    let t: ReturnType<typeof setInterval> | undefined;

    async function appendMessages() {
      const { messages: newMessages = [], error } = await getTopicMessages(
        kafkaId,
        topicId,
        {
          pageSize: 50,
          query,
          where,
          partition,
          filter: {
            type: "timestamp",
            value: previousTs.current,
          },
        },
      );
      if (!error) {
        const sortedMessages = newMessages
          .sort(
            (a, b) =>
              new Date(b.attributes.timestamp).getTime() -
              new Date(a.attributes.timestamp).getTime(),
          )
          .sort((a, b) => b.attributes.offset - a.attributes.offset);
        return {
          messages: sortedMessages,
          ts: sortedMessages[0]?.attributes.timestamp,
        };
      }
    }

    async function tick() {
      // console.log("tick", {
      //   ts: Date.now(),
      //   fetching: isFetching.current,
      //   kafkaId,
      //   topicId,
      //   partition,
      //   query,
      //   where,
      // });
      if (!isFetching.current && !isPaused) {
        isFetching.current = true;
        const res = await appendMessages();
        if (!isPaused && res) {
          if (res.ts) {
            previousTs.current = res.ts;
          }
          onUpdates(res.messages, new Date());
        }
        isFetching.current = false;
      }
    }

    t = setInterval(tick, 1000);
    void tick();

    return () => {
      // console.log("destroy", {
      //   kafkaId,
      //   topicId,
      //   partition,
      //   query,
      //   where,
      // });
      clearInterval(t);
      t = undefined;
    };
  }, [isPaused, kafkaId, onUpdates, partition, query, topicId, where]);

  return (
    <AlertContinuousMode
      isPaused={isPaused}
      onToggle={() => setIsPaused((p) => !p)}
    />
  );
}
