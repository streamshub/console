import { useFormatBytes } from "@/utils/format";

export function Bytes({ value }: { value: number | undefined }) {
  const formatter = useFormatBytes();
  return value ? formatter(value) : "-";
}
