import { useEffect, useRef, useState } from 'react';
import { formatInTimeZone } from 'date-fns-tz';
import {
  Button,
  CalendarMonth,
  Flex,
  FlexItem,
  FormHelperText,
  HelperText,
  HelperTextItem,
  InputGroup,
  InputGroupItem,
  isValidDate,
  MenuToggle,
  MenuToggleElement,
  Panel,
  PanelMain,
  PanelMainBody,
  Popper,
  Select,
  SelectList,
  SelectOption,
  TextInput,
} from '@patternfly/react-core';
import OutlinedClockIcon from '@patternfly/react-icons/dist/esm/icons/outlined-clock-icon';

type Zone = 'UTC' | 'local';

const LOCAL_TZ = Intl.DateTimeFormat().resolvedOptions().timeZone;
const ZERO_TIME = '00:00:00.000';

// The popup is exactly as wide as the icon + input. The calendar is wider than
// that by default, so it is scaled down to fit inside the popup padding.
const TRIGGER_WIDTH_REM = 21;
const POPUP_PADDING_REM = 2; // 1rem on each side
const CALENDAR_NATURAL_WIDTH_REM = 24; // adjust if your calendar is a different width
const CALENDAR_ZOOM = Math.min(
  1,
  (TRIGGER_WIDTH_REM - POPUP_PADDING_REM) / CALENDAR_NATURAL_WIDTH_REM
);
const pad = (n: number) => String(n).padStart(2, '0');

// yyyy-MM-ddTHH:mm:ss.SSSZ (UTC) or yyyy-MM-ddTHH:mm:ss.SSS±HH:mm (local time)
const ISO_REGEX =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})\.(\d{3})(Z|[+-]\d{2}:\d{2})$/;

/** Only the two accepted formats pass; impossible dates such as Feb 31 fail. */
export function isValidISODateTime(value: string): boolean {
  const match = ISO_REGEX.exec(value);
  if (!match) return false;

  const [, y, mo, d, h, mi, s, , offset] = match;
  const year = Number(y);
  const month = Number(mo);
  const day = Number(d);

  const calendarDate = new Date(Date.UTC(year, month - 1, day));
  if (
    calendarDate.getUTCFullYear() !== year ||
    calendarDate.getUTCMonth() !== month - 1 ||
    calendarDate.getUTCDate() !== day
  ) {
    return false;
  }
  if (Number(h) > 23 || Number(mi) > 59 || Number(s) > 59) return false;

  if (offset !== 'Z') {
    const [oh, om] = offset.slice(1).split(':').map(Number);
    if (oh > 23 || om > 59) return false;
  }

  return !Number.isNaN(new Date(value).getTime());
}

/** "" -> 00:00:00.000, "9:30" -> 09:30:00.000, invalid -> undefined. */
function normalizeTime(raw: string): string | undefined {
  const text = raw.trim();
  if (text === '') return ZERO_TIME;

  const match = /^(\d{1,2}):(\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?$/.exec(text);
  if (!match) return undefined;

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  const seconds = Number(match[3] ?? 0);
  if (hours > 23 || minutes > 59 || seconds > 59) return undefined;

  const millis = (match[4] ?? '').padEnd(3, '0');
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}.${millis}`;
}

/** Combine the calendar day, the time and the zone into an accepted timestamp. */
function buildTimestamp(date: Date, time: string, zone: Zone): string {
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}`;
  if (zone === 'UTC') return `${day}T${time}Z`;

  const [hh, mm, rest] = time.split(':');
  const [ss, ms] = rest.split('.');
  const local = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    Number(hh),
    Number(mm),
    Number(ss),
    Number(ms)
  );
  return formatInTimeZone(local, LOCAL_TZ, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx");
}

/** Read the wall-clock parts of a valid timestamp back into picker fields. */
function toDraft(value: string) {
  if (!isValidISODateTime(value)) return undefined;
  const [datePart, timePart] = value.split('T');
  const [y, m, d] = datePart.split('-').map(Number);
  return {
    date: new Date(y, m - 1, d),
    time: timePart.slice(0, 12),
    zone: (value.endsWith('Z') ? 'UTC' : 'local') as Zone,
  };
}

interface TimestampPickerProps {
  value?: string;
  onCommit: (value: string | undefined) => void;
}

export function TimestampPicker({ value, onCommit }: TimestampPickerProps) {
  // The text box
  const [text, setText] = useState(value ?? '');
  const [prevValue, setPrevValue] = useState(value);
  const [textError, setTextError] = useState(false);

  // The popup
  const [isOpen, setIsOpen] = useState(false);
  const [isZoneOpen, setIsZoneOpen] = useState(false);
  const [popupWidth, setPopupWidth] = useState<number>();
  const [date, setDate] = useState<Date>(() => new Date());
  const [time, setTime] = useState(ZERO_TIME);
  const [zone, setZone] = useState<Zone>('UTC');
  const [timeError, setTimeError] = useState(false);

  const triggerRef = useRef<HTMLDivElement>(null);
  const popperRef = useRef<HTMLDivElement>(null);

  // Follow changes made from outside (reset, reload)
  if (value !== prevValue) {
    setPrevValue(value);
    setText(value ?? '');
    setTextError(false);
  }

  // Close on outside click or Escape
  useEffect(() => {
    if (!isOpen) return;
    const onMouseDown = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        !triggerRef.current?.contains(target) &&
        !popperRef.current?.contains(target)
      ) {
        setIsOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('mousedown', onMouseDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onMouseDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [isOpen]);

  // Typed text: only the two accepted formats are applied
  const commitText = () => {
    const trimmed = text.trim();
    if (trimmed === '') {
      setTextError(false);
      if (value !== undefined) onCommit(undefined);
    } else if (isValidISODateTime(trimmed)) {
      setTextError(false);
      if (trimmed !== value) onCommit(trimmed);
    } else {
      setTextError(true);
    }
  };

  const openPicker = () => {
    const draft = toDraft(text.trim());
    setDate(draft?.date ?? new Date());
    setTime(draft?.time ?? ZERO_TIME);
    setZone(draft?.zone ?? 'UTC');
    setTimeError(false);
    setPopupWidth(triggerRef.current?.getBoundingClientRect().width);
    setIsOpen(true);
  };

  const confirm = () => {
    const normalized = normalizeTime(time);
    if (!normalized) {
      setTimeError(true);
      return;
    }
    const timestamp = buildTimestamp(date, normalized, zone);
    setText(timestamp);
    setTextError(false);
    onCommit(timestamp);
    setIsOpen(false);
  };

  const trigger = (
    <div ref={triggerRef} style={{ width: `${TRIGGER_WIDTH_REM}rem` }}>
      <InputGroup>
        <InputGroupItem>
          <Button
            variant="control"
            aria-label="Open timestamp picker"
            icon={<OutlinedClockIcon />}
            onClick={() => (isOpen ? setIsOpen(false) : openPicker())}
          />
        </InputGroupItem>
        <InputGroupItem isFill>
          <TextInput
            aria-label="Timestamp"
            placeholder="yyyy-MM-ddTHH:mm:ss.SSS±HH:mm"
            value={text}
            validated={textError ? 'error' : 'default'}
            onChange={(_, next) => {
              setText(next);
              setTextError(false);
            }}
            onBlur={commitText}
            onKeyDown={(e) => e.key === 'Enter' && commitText()}
          />
        </InputGroupItem>
      </InputGroup>
      {textError && (
        <FormHelperText>
          <HelperText>
            <HelperTextItem variant="error">
              Invalid timestamp. Use yyyy-MM-ddTHH:mm:ss.SSSZ (UTC) or
              yyyy-MM-ddTHH:mm:ss.SSS±HH:mm (local time)
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      )}
    </div>
  );

  const popper = (
    <div ref={popperRef} style={{ width: popupWidth }}>
      <Panel variant="raised">
        <PanelMain>
          <PanelMainBody>
            <Flex
              direction={{ default: 'column' }}
              spaceItems={{ default: 'spaceItemsMd' }}
            >
              <FlexItem>
                <Select
                  isOpen={isZoneOpen}
                  onOpenChange={setIsZoneOpen}
                  popperProps={{ appendTo: 'inline' }}
                  selected={zone}
                  onSelect={(_, selection) => {
                    setZone(selection as Zone);
                    setIsZoneOpen(false);
                  }}
                  toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                    <MenuToggle
                      ref={toggleRef}
                      isFullWidth
                      isExpanded={isZoneOpen}
                      onClick={() => setIsZoneOpen((open) => !open)}
                      aria-label="Time zone"
                    >
                      {zone === 'UTC' ? 'UTC' : 'Local time'}
                    </MenuToggle>
                  )}
                >
                  <SelectList>
                    <SelectOption value="UTC">UTC</SelectOption>
                    <SelectOption value="local">Local time</SelectOption>
                  </SelectList>
                </Select>
              </FlexItem>

              <FlexItem alignSelf={{ default: 'alignSelfCenter' }}>
                <div style={{ zoom: CALENDAR_ZOOM }}>
                  <CalendarMonth
                    date={date}
                    onChange={(_, newDate) => {
                      if (isValidDate(newDate)) setDate(newDate);
                    }}
                  />
                </div>
              </FlexItem>

              <FlexItem>
                <label htmlFor="timestamp-time">Time (optional)</label>
                <TextInput
                  id="timestamp-time"
                  value={time}
                  placeholder="HH:mm:ss.SSS"
                  validated={timeError ? 'error' : 'default'}
                  onChange={(_, next) => {
                    setTime(next);
                    setTimeError(false);
                  }}
                  onKeyDown={(e) => e.key === 'Enter' && confirm()}
                />
                {timeError && (
                  <FormHelperText>
                    <HelperText>
                      <HelperTextItem variant="error">
                        Use HH:mm:ss.SSS, for example {ZERO_TIME}
                      </HelperTextItem>
                    </HelperText>
                  </FormHelperText>
                )}
              </FlexItem>

              <FlexItem>
                <Button variant="primary" onClick={confirm}>
                  Confirm
                </Button>{' '}
                <Button variant="link" onClick={() => setIsOpen(false)}>
                  Cancel
                </Button>
              </FlexItem>
            </Flex>
          </PanelMainBody>
        </PanelMain>
      </Panel>
    </div>
  );

  return (
    <Popper
      trigger={trigger}
      triggerRef={triggerRef}
      popper={popper}
      popperRef={popperRef}
      isVisible={isOpen}
      appendTo={() => document.body}
      placement="bottom-start"
    />
  );
}
