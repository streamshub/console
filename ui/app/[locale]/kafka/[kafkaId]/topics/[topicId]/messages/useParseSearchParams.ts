import { parseSearchParams } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/parseSearchParams";
import { useSearchParams } from "next/navigation";
import { useMemo } from "react";

export function useParseSearchParams(): [
  ReturnType<typeof parseSearchParams>,
  Record<string, string>,
] {
  const searchParamsEntities = useSearchParams();
  return useMemo(() => {
    const searchParams = Object.fromEntries(searchParamsEntities);
    return [parseSearchParams(searchParams), searchParams];
  }, [searchParamsEntities]);
}
