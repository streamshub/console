"use client";
import { MessagesTableSkeleton } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import {
  MessagesSearchParams,
  parseSearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { useSearchParams } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";

export default function MessagesLoading({ children }: PropsWithChildren) {
  const searchParams = useSearchParams();
  const { partition, offset, timestamp, epoch, limit } = parseSearchParams(
    Object.fromEntries(searchParams.entries()) as MessagesSearchParams,
  );
  return (
    <Suspense
      fallback={
        <MessagesTableSkeleton
          limit={limit}
          partition={partition}
          filterTimestamp={timestamp}
          filterEpoch={epoch}
          filterOffset={offset}
        />
      }
    >
      {children}
    </Suspense>
  );
}
