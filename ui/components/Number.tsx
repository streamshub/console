"use client";

import { useFormatter } from "next-intl";

export function Number({ value }: { value: number | undefined }) {
  const formatter = useFormatter();
  return value ? formatter.number(value) : "-";
}
