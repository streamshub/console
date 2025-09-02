import { formatInTimeZone } from "date-fns-tz";

export function formatDateTime({
  value,
  timeZone,
  format = "yyyy-MM-dd HH:mm:ssXXX",
} : {
  value: number | string | Date | undefined,
  timeZone?: string,
  format?: string,
}) {
  if (value === undefined) {
    return "-";
  }

  timeZone ??= Intl.DateTimeFormat().resolvedOptions().timeZone;

  return formatInTimeZone(value, timeZone, format);
}
