import { stringToInt } from "@/utils/stringToInt";
import { useSearchParams } from "next/navigation";
import { useMemo } from "react";

export type MessagesSearchParams = {
  limit?: string;
  partition?: string;
  selected?: string;
  query?: string;
  "filter[offset]"?: string;
  "filter[timestamp]"?: string;
  "filter[epoch]"?: string;
};

export function parseSearchParams(searchParams: MessagesSearchParams) {
  const limit = stringToInt(searchParams.limit) || 50;
  const offset = stringToInt(searchParams["filter[offset]"]);
  const ts = stringToInt(searchParams["filter[timestamp]"]);
  const epoch = stringToInt(searchParams["filter[epoch]"]);
  const selected = searchParams.selected;
  const query = searchParams.query;
  const partition = stringToInt(searchParams.partition);

  const timeFilter = epoch ? epoch * 1000 : ts;
  const date = timeFilter ? new Date(timeFilter) : undefined;
  const timestamp = date?.toISOString();

  const [selectedPartition, selectedOffset] = selected
    ? decodeURIComponent(selected).split(":").map(stringToInt)
    : [undefined, undefined];

  return {
    limit,
    offset,
    timestamp,
    epoch,
    selectedOffset,
    selectedPartition,
    partition,
    query,
  };
}

export function useParseSearchParams() {
  const searchParamsEntities = useSearchParams();
  return useMemo(() => {
    const searchParams = Object.fromEntries(searchParamsEntities);
    return parseSearchParams(searchParams);
  }, [searchParamsEntities]);
}
