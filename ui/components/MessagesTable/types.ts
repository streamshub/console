export type SearchParams = {
  partition?: number;
  limit: number | "continuously";
  query?: {
    value: string;
    where: "headers" | "key" | "value" | "everywhere" | `jq:${string}`;
  };
  from:
    | { type: "timestamp"; value: string }
    | { type: "epoch"; value: number }
    | { type: "offset"; value: number }
    | { type: "latest" };
};
