import { useFormatBytes } from "@/utils/format";

export function Bytes({ value }: { value: string | number | null | undefined }) {
  const formatter = useFormatBytes();
  if (value === undefined || value === null) {
    return "-";
  }
  value = typeof value === "string" ? parseInt(value, 10) : value;
  return isNaN(value) ? "-" : formatter(value);
}
