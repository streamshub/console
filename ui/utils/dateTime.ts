import { format as formatDate } from "date-fns";
import { formatInTimeZone } from "date-fns-tz";

const FORMAT = "yyyy-MM-dd HH:mm:ssXXX";

export function formatDateTime(value: number | string | Date | undefined, format: string = FORMAT, utc?: boolean) {
  if (value === undefined) {
    return "-";
  }

  if (utc) {
    return formatInTimeZone(value, "UTC", format);
  } else {
    return formatDate(value, format);
  }
}
