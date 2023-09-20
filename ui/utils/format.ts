import convert from "convert";
import { useFormatter } from "next-intl";

export function useFormatBytes() {
  const format = useFormatter();
  return function formatBytes(bytes: number) {
    const res = convert(bytes, "bytes").to("best");
    const unit: Record<(typeof res)["unit"], string> = {
      bits: "bit",
      GiB: "gigabyte",
      KiB: "kilobyte",
      GB: "gigabyte",
      MiB: "megabyte",
      PiB: "petabyte",
      TiB: "terabyte",
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
