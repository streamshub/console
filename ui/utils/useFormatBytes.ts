import convert from "convert";
import { useFormatter } from "next-intl";

export function useFormatBytes() {
  const format = useFormatter();
  return function formatBytes(
    bytes: number,
    { maximumFractionDigits }: { maximumFractionDigits?: number } = {},
  ) {
    if (bytes == 0) {
      return "0 B";
    }
    const res = convert(bytes, "bytes").to("best", "imperial");
    let minimumFractionDigits = undefined;

    if (maximumFractionDigits === undefined) {
      switch (res.unit) {
        case "PiB":
        case "TiB":
        case "GiB":
        case "MiB":
        case "KiB":
          if (res.quantity >= 100) {
            maximumFractionDigits = 0;
          } else if (res.quantity >= 10) {
            minimumFractionDigits = 1;
            maximumFractionDigits = 1;
          } else {
            minimumFractionDigits = 2;
            maximumFractionDigits = 2;
          }
          break;
        default:
          maximumFractionDigits = 0;
          break;
      }
    }

    return `${format.number(res.quantity, {
      style: "decimal",
      minimumFractionDigits,
      maximumFractionDigits,
    })} ${res.unit}`;
  };
}
