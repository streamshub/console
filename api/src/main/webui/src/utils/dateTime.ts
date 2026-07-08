/**
 * Date/Time formatting utilities
 */

import { formatInTimeZone } from 'date-fns-tz';

/**
 * Convert an ISO 8601 string to the "YYYY-MM-DDTHH:mm" format required by
 * <input type="datetime-local">, expressed in the user's local timezone.
 */
export function toDatetimeLocal(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const localTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return formatInTimeZone(d, localTz, "yyyy-MM-dd'T'HH:mm");
}

/**
 * Convert a "YYYY-MM-DDTHH:mm" datetime-local string to a full ISO 8601
 * string with the local timezone offset (e.g. "2024-06-15T14:30:00.000+02:00").
 * Preserves the user's intent rather than silently converting to UTC.
 */
export function toISOWithLocalOffset(datetimeLocal: string): string {
  const d = new Date(datetimeLocal);
  if (isNaN(d.getTime())) return datetimeLocal;
  const localTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return formatInTimeZone(d, localTz, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
}

export function formatDateTime({
  value,
  timeZone,
  format = 'yyyy-MM-dd HH:mm:ssXXX',
}: {
  value: number | string | Date | undefined;
  timeZone?: string;
  format?: string;
}) {
  if (value === undefined) {
    return '-';
  }

  timeZone ??= Intl.DateTimeFormat().resolvedOptions().timeZone;

  return formatInTimeZone(value, timeZone, format);
}