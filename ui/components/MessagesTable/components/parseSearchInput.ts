import { SearchParams } from "../types";

export function parseSearchInput({ value }: { value: string }): SearchParams {
  let sp: SearchParams = {
    from: {
      type: "latest",
    },
    limit: 50,
    partition: undefined,
    query: {
      where: "everywhere",
      value: "",
    },
  };
  const parts = value.split(" ");
  let queryParts: string[] = [];
  parts.forEach((p) => {
    if (p.indexOf(`where=`) === 0) {
      const [_, where] = p.split("=");
      sp.query!.where = parseWhere(where);
    } else if (p.indexOf(`messages=`) === 0) {
      const [_, from] = p.split("=");
      if (from === "latest") {
        sp.from = {
          type: "latest",
        };
      } else {
        const [type, ...valueParts] = from.split(":");
        const value = valueParts.join(":");
        switch (type) {
          case "offset": {
            const number = parseInt(value, 10);
            if (Number.isSafeInteger(number)) {
              sp.from = {
                type: "offset",
                value: number,
              };
            }
            break;
          }
          case "epoch": {
            const number = parseInt(value, 10);
            if (Number.isSafeInteger(number)) {
              sp.from = {
                type: "epoch",
                value: number,
              };
            }
            break;
          }
          case "timestamp": {
            sp.from = {
              type: "timestamp",
              value,
            };
            break;
          }
        }
      }
    } else if (p.indexOf("retrieve=") === 0) {
      const [_, limit] = p.split("=");
      if (limit === "continuously") {
        sp.limit = "continuously";
      } else {
        const number = parseInt(limit, 10);
        if (Number.isSafeInteger(number)) {
          sp.limit = number;
        }
      }
    } else if (p.indexOf("partition=") === 0) {
      const [_, partition] = p.split("=");
      const number = parseInt(partition, 10);
      if (Number.isSafeInteger(number)) {
        sp.partition = number;
      }
    } else {
      queryParts.push(p);
    }
  });
  const query = queryParts.join(" ").trim();

  if (query.length > 0) {
    sp.query!.value = query;
  }

  return sp;
}

export function parseWhere(where: string | undefined) {
  switch (where) {
    case "key":
      return "key" as const;
    case "headers":
      return "headers" as const;
    case "value":
      return "value" as const;
    default:
      return "everywhere" as const;
  }
}
