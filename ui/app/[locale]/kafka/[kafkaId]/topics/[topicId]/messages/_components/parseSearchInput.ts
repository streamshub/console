import { SearchParams } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";

export function parseSearchInput({ value }: { value: string }): SearchParams {
  let sp: SearchParams = {
    from: {
      type: "latest",
    },
    until: {
      type: "limit",
      value: 50,
    },
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
    } else if (p.indexOf(`from=`) === 0) {
      const [_, from] = p.split("=");
      if (from === "now") {
        sp.from = {
          type: "latest",
        };
      } else {
        const [type, value] = from.split(":");
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
    } else if (p.indexOf("until=") === 0) {
      const [_, until] = p.split("=");
      const [type, value] = until.split(":");
      switch (type) {
        case "limit": {
          const number = parseInt(value, 10);
          if (Number.isSafeInteger(number)) {
            sp.until = {
              type: "limit",
              value: number,
            };
          }
          break;
        }
        // case "partition": {
        //   sp.until = {
        //     type: "partition",
        //     value
        //   }
        //   break;
        // }
        case "timestamp": {
          sp.until = {
            type: "timestamp",
            value,
          };
          break;
        }
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
      if (where?.indexOf("jq:") === 0) {
        return where as `jq:${string}`;
      }
      return "everywhere" as const;
  }
}
