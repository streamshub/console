"use client";

import { formatDateTime } from "@/utils/dateTime";
import { isDate, parseISO } from "date-fns";
import { ReactNode, useEffect, useState } from "react";

const FORMAT = "yyyy-MM-dd HH:mm:ssXXX";

export function DateTime({
  value,
  utc = false,
  empty = "-",
}: {
  readonly value: string | Date | undefined;
  readonly utc?: boolean;
  readonly empty?: ReactNode;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  if (!mounted) {
    // Do not return any result unless mounted (i.e., running client side)
    return null;
  }

  if (!value) {
    return empty;
  }

  const dateValue = typeof value === "string" ? parseISO(value) : value;

  if (!isDate(dateValue)) {
    return empty;
  }

  return (
    <time dateTime={dateValue.toISOString()}>
      {formatDateTime(value, FORMAT, utc)}
    </time>
  );
}
