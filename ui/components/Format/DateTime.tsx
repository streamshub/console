import { isDate, parseISO } from "date-fns";
import { useFormatter } from "next-intl";
import { ReactNode } from "react";

export function DateTime({
  value,
  dateStyle = "long",
  timeStyle = "long",
  tz = "local",
  empty = "-",
}: {
  value: string | Date | undefined;
  dateStyle?: "full" | "long" | "medium" | "short";
  timeStyle?: "full" | "long" | "medium" | "short";
  tz?: "UTC" | "local";
  empty?: ReactNode;
}) {
  const format = useFormatter();
  if (!value) {
    return empty;
  }
  const maybeDate = typeof value === "string" ? parseISO(value) : value;
  if (!isDate(maybeDate)) {
    return empty;
  }
  return format.dateTime(maybeDate, {
    dateStyle,
    timeStyle,
    timeZone: tz === "local" ? undefined : "UTC",
  });
}
