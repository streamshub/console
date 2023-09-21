import convert from "convert";
import { useFormatter } from "next-intl";

export function useFormatBytes() {
  const format = useFormatter();
  return function formatBytes(bytes: number) {
    const res = convert(bytes, "bytes").to("best", "metric");
    const unit: Record<(typeof res)["unit"], string> = {
      bits: "bit",
      GB: "gigabyte",
      TB: "terabyte",
      PB: "petabyte",
      MB: "megabyte",
      KB: "kilobyte",
      B: "byte",
    };
    return format.number(res.quantity, {
      style: "unit",
      unit: unit[res.unit],
      unitDisplay: "narrow",
    });
  };
}
