export type SearchParams = {
  partition?: number;
  query?: {
    value: string;
    where: "headers" | "key" | "value" | "everywhere" | `jq:${string}`;
  };
  from:
    | { type: "timestamp"; value: string }
    | { type: "epoch"; value: number }
    | { type: "offset"; value: number }
    | { type: "latest" };
  until:
    | { type: "limit"; value: number }
    | { type: "live" }
    | { type: "timestamp"; value: string }
    | {
        type: "partition";
        value: number;
      };
};
