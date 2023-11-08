"use client";
import { MessagesTableSkeleton } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { useSearchParams } from "next/navigation";

export default function MessagesLoading() {
  const searchParams = useSearchParams();
  const { partition, offset, timestamp, epoch, limit } = parseSearchParams(
    Object.fromEntries(searchParams.entries()) as MessagesSearchParams,
  );
  return (
    <MessagesTableSkeleton
      limit={limit}
      partition={partition}
      filterTimestamp={timestamp}
      filterEpoch={epoch}
      filterOffset={offset}
    />
  );
}
