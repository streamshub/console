"use client";

import { useFormatter } from "next-intl";

export function Number({ value }: { value: string | number | null | undefined }) {
  const formatter = useFormatter();
  value = typeof value === "string" ? parseInt(value, 10) : value;
  return value !== undefined && value !== null && !isNaN(value) ? formatter.number(value) : "-";
}
