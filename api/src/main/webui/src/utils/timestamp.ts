export type Zone = 'UTC' | 'local';

export interface Draft {
  date: Date;
  time: string;
  zone: Zone;
}

export const DEFAULT_TIME = '00:00:00.000';

const OFFSET = '[+-](?:[01]\\d|2[0-3]):[0-5]\\d';
const HMS = '([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)';

const TIMESTAMP_RE = new RegExp(
  `^(\\d{4})-(\\d{2})-(\\d{2})T${HMS}(?:\\.(\\d{1,3}))?(Z|${OFFSET})$`
);

const TIME_RE = new RegExp(
  `^([01]?\\d|2[0-3]):([0-5]\\d)(?::([0-5]\\d)(?:\\.(\\d{1,3}))?)?\\s*(${OFFSET})?$`
);

const pad = (n: number) => String(n).padStart(2, '0');

function isRealDate(y: number, m: number, d: number) {
  const dt = new Date(0);
  dt.setUTCFullYear(y, m - 1, d);
  return (
    dt.getUTCFullYear() === y &&
    dt.getUTCMonth() === m - 1 &&
    dt.getUTCDate() === d
  );
}

export function normalizeTimestamp(text: string): string | undefined {
  const m = TIMESTAMP_RE.exec(text.trim());
  if (!m) return undefined;

  const [, y, mo, d, hh, mm, ss, ms = '', offset] = m;
  if (!isRealDate(+y, +mo, +d)) return undefined;

  return `${y}-${mo}-${d}T${hh}:${mm}:${ss}.${ms.padEnd(3, '0')}${offset}`;
}

export function parseTime(raw: string, zone: Zone) {
  const trimmed = raw.trim();
  if (!trimmed) return { time: DEFAULT_TIME, offset: undefined };

  const m = TIME_RE.exec(trimmed);
  if (!m) return undefined;

  const [, h, min, s = '00', ms = '', offset] = m;
  if (offset && zone === 'UTC') return undefined;

  return { time: `${pad(+h)}:${min}:${s}.${ms.padEnd(3, '0')}`, offset };
}

export function stripOffset(time: string) {
  return time.replace(/\s*[+-]\d{2}:\d{2}$/, '');
}

export function machineOffset(date: Date, time: string) {
  const [hh = 0, mm = 0, ss = 0] = time.split(/[:.]/).map(Number);
  const minutes = -new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    hh,
    mm,
    ss
  ).getTimezoneOffset();
  const abs = Math.abs(minutes);
  return `${minutes >= 0 ? '+' : '-'}${pad(Math.floor(abs / 60))}:${pad(
    abs % 60
  )}`;
}

export function buildTimestamp({ date, time, zone }: Draft) {
  const parsed = parseTime(time, zone);
  if (!parsed) return undefined;

  const offset =
    zone === 'UTC' ? 'Z' : parsed.offset ?? machineOffset(date, parsed.time);
  const day = `${String(date.getFullYear()).padStart(4, '0')}-${pad(
    date.getMonth() + 1
  )}-${pad(date.getDate())}`;

  return normalizeTimestamp(`${day}T${parsed.time}${offset}`);
}

export function toDraft(text: string | undefined): Draft | undefined {
  const ts = text ? normalizeTimestamp(text) : undefined;
  if (!ts) return undefined;

  const [y, mo, d] = ts.slice(0, 10).split('-').map(Number);
  const time = ts.slice(11, 23);
  const offset = ts.slice(23);
  const isUtc = offset === 'Z' || offset === '+00:00';

  return {
    date: new Date(y, mo - 1, d),
    time: isUtc ? time : `${time}${offset}`,
    zone: isUtc ? 'UTC' : 'local',
  };
}

export function defaultDraft(): Draft {
  const now = new Date();
  return {
    date: new Date(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
    time: DEFAULT_TIME,
    zone: 'UTC',
  };
}
