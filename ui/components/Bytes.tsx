import { useFormatBytes } from "@/utils/format";

export function Bytes({ value }: { value: string | number | undefined }) {
  const formatter = useFormatBytes();
  value = typeof value === "string" ? parseInt(value, 10) : value;
  return value !== undefined ? formatter(value) : "-";
}
