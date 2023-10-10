"use client";

import { useFormatter } from "next-intl";

export function Number({ value }: { value: number | undefined }) {
  const formatter = useFormatter();
  return (typeof value !== 'undefined') ? formatter.number(value) : "-";
}
