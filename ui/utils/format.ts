import convert from "convert";
import { useFormatter } from "next-intl";

export function useFormatBytes() {
  const format = useFormatter();
  return function formatBytes(bytes: number) {
    const res = convert(bytes, "bytes").to("best", "imperial");
    return `${format.number(res.quantity, {
      style: "decimal",
    })} ${res.unit}`;
  };
}
