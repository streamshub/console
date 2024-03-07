import { stringToInt } from "@/utils/stringToInt";
import { useSearchParams } from "next/navigation";
import { useMemo } from "react";

export type MessagesSearchParams = {
  retrieve?: string;
  partition?: string;
  selected?: string;
  query?: string;
  where?: string;
  offset?: string;
  timestamp?: string;
  epoch?: string;
  _?: string;
};

export function parseSearchParams(searchParams: MessagesSearchParams) {
  const _ = searchParams._;
  const limit =
    searchParams.retrieve === "continuously"
      ? ("continuously" as const)
      : stringToInt(searchParams.retrieve);
  const offset = stringToInt(searchParams["offset"]);
  const timestamp = searchParams["timestamp"];
  const epoch = stringToInt(searchParams["epoch"]);
  const selected = searchParams.selected;
  const query = searchParams.query;
  const where = (() => {
    switch (searchParams.where) {
      case "key":
        return "key" as const;
      case "headers":
        return "headers" as const;
      case "value":
        return "value" as const;
      default:
        return undefined;
    }
  })();
  const partition = stringToInt(searchParams.partition);

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
    where,
    _,
  };
}

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
