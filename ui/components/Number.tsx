"use client";

import { useFormatter } from "next-intl";

export function Number({ value }: { value: number }) {
  const formatter = useFormatter();
  return formatter.number(value);
}
