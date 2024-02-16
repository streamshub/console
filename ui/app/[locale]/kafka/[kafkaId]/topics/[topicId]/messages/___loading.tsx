"use client";
import { MessagesTableSkeleton } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import { parseSearchParams } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { useSearchParams } from "next/navigation";

export default function MessagesLoading() {
  const searchParamsEntries = useSearchParams();
  let searchParams;
  try {
    searchParams = Object.fromEntries(searchParamsEntries);
  } catch (e) {
    console.error(e);
    searchParams = {};
  }
  const { partition, offset, timestamp, epoch, limit } =
    parseSearchParams(searchParams);
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
