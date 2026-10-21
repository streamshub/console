import { useCallback, useEffect, useRef, useState } from 'react';
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

const TRIGGER_WIDTH_REM = 20;
const pad = (n: number) => String(n).padStart(2, '0');

const ISO_REGEX =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})\.(\d{3})(Z|[+-]\d{2}:\d{2})$/;

function normalizeISOInput(raw: string): string {
  const trimmed = raw.trim();

  // Allow a space instead of the ISO 8601 "T" delimiter.
  return trimmed.replace(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:)/, '$1T$2');
}

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
  const [text, setText] = useState(value ?? '');
  const [prevValue, setPrevValue] = useState(value);
  const [textError, setTextError] = useState(false);

  const [isOpen, setIsOpen] = useState(false);
  const [isZoneOpen, setIsZoneOpen] = useState(false);
  const [popupWidth, setPopupWidth] = useState<number>();
  const [calendarZoom, setCalendarZoom] = useState(1);
  const [date, setDate] = useState<Date>(() => new Date());
  const [time, setTime] = useState(ZERO_TIME);
  const [zone, setZone] = useState<Zone>('UTC');
  const [timeError, setTimeError] = useState(false);

  const triggerRef = useRef<HTMLDivElement>(null);
  const popperRef = useRef<HTMLDivElement>(null);
  const calendarMeasured = useRef(false);

  const fitCalendar = useCallback((node: HTMLDivElement | null) => {
    if (!node || calendarMeasured.current) return;
    const available = node.parentElement?.clientWidth;
    const natural = node.offsetWidth;
    if (!available || !natural) return;
    calendarMeasured.current = true;
    setCalendarZoom(Math.min(1, (available - 1) / natural));
  }, []);

  if (value !== prevValue) {
    setPrevValue(value);
    setText(value ?? '');
    setTextError(false);
  }

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

  const commitText = () => {
    const normalized = normalizeISOInput(text);

    if (normalized === '') {
      setTextError(false);
      if (value !== undefined) onCommit(undefined);
    } else if (isValidISODateTime(normalized)) {
      setTextError(false);

      // Show the normalized ISO format in the input.
      setText(normalized);

      if (normalized !== value) {
        onCommit(normalized);
      }
    } else {
      setTextError(true);
    }
  };

  const openPicker = () => {
    const draft = toDraft(normalizeISOInput(text));
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
            placeholder="yyyy-MM-ddTHH:mm:ss.SSS"
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
              Invalid ISO 8601 format. Expected: yyyy-MM-ddTHH:mm:ss.SSSZ
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
                <HelperText>
                  <HelperTextItem>
                    Format:{' '}
                    {zone === 'UTC'
                      ? 'yyyy-MM-ddTHH:mm:ss.SSSZ'
                      : 'yyyy-MM-ddTHH:mm:ss.SSS±HH:mm'}
                  </HelperTextItem>
                </HelperText>
              </FlexItem>

              <FlexItem>
                <div
                  ref={fitCalendar}
                  style={{
                    zoom: calendarZoom,
                    width: 'max-content',
                    margin: '0 auto',
                  }}
                >
                  <CalendarMonth
                    date={date}
                    onChange={(_, newDate) => {
                      if (isValidDate(newDate)) setDate(newDate);
                    }}
                  />
                </div>
              </FlexItem>

              <FlexItem>
                <label htmlFor="timestamp-time">Time</label>
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
                        Use HH:mm:ss.SSS
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
