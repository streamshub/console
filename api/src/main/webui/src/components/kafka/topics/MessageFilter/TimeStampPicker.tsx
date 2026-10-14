import { useEffect, useRef, useState } from 'react';
import {
  Button,
  CalendarMonth,
  Flex,
  FlexItem,
  InputGroup,
  InputGroupItem,
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

const TIMESTAMP_REGEX =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})\.(\d{3})(Z|[+-]\d{2}:\d{2})$/;

const TIME_REGEX = /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)\.(\d{3})$/;

const TIME_LOOSE =
  /^([01]?\d|2[0-3]):([0-5]\d)(?::([0-5]\d)(?:\.(\d{1,3}))?)?$/;

const pad = (n: number) => String(n).padStart(2, '0');

function normalizeTime(raw: string) {
  const trimmed = raw.trim();

  // Default to midnight when no time is entered.
  if (!trimmed) return '00:00:00.000';

  const match = TIME_LOOSE.exec(trimmed);
  if (!match) return undefined;

  const [, h, min, s = '00', ms = ''] = match;

  return `${pad(+h)}:${min}:${s}.${ms.padEnd(3, '0')}`;
}

function formatOffset(minutes: number) {
  const sign = minutes >= 0 ? '+' : '-';
  const abs = Math.abs(minutes);
  return `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`;
}

/** Builds yyyy-MM-ddTHH:mm:ss.SSS±HH:mm from the popover fields. */
function buildTimestamp(date: Date, rawTime: string, zone: Zone) {
  const time = normalizeTime(rawTime);
  if (!time) return undefined;

  const match = TIME_REGEX.exec(time);
  if (!match) return undefined;

  const [, hh, mm, ss, ms] = match;
  const y = date.getFullYear();
  const mo = date.getMonth();
  const d = date.getDate();

  const offset =
    zone === 'UTC'
      ? '+00:00'
      : formatOffset(
          -new Date(
            y,
            mo,
            d,
            Number(hh),
            Number(mm),
            Number(ss)
          ).getTimezoneOffset()
        );

  const result =
    `${y}-${pad(mo + 1)}-${pad(d)}` + `T${hh}:${mm}:${ss}.${ms}${offset}`;

  return Number.isNaN(Date.parse(result)) ? undefined : result;
}
/** Splits an existing timestamp back into popover fields. */
function parseTimestamp(text: string) {
  const m = TIMESTAMP_REGEX.exec(text.trim());
  if (!m || Number.isNaN(Date.parse(text))) return undefined;
  const [, y, mo, d, hh, mm, ss, ms, off] = m;
  return {
    date: new Date(+y, +mo - 1, +d),
    time: `${hh}:${mm}:${ss}.${ms}`,
    zone: (off === 'Z' || off === '+00:00' ? 'UTC' : 'local') as Zone,
  };
}

function nowParts() {
  const now = new Date();
  return {
    date: new Date(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
    time: now.toISOString().slice(11, 23), // HH:mm:ss.SSS in UTC
    zone: 'UTC' as Zone,
  };
}

interface TimestampPickerProps {
  value?: string;
  onCommit: (value: string | undefined) => void;
}

export function TimestampPicker({ value, onCommit }: TimestampPickerProps) {
  const [text, setText] = useState(value ?? '');
  const [prevValue, setPrevValue] = useState(value);
  const [isOpen, setIsOpen] = useState(false);
  const [isZoneOpen, setIsZoneOpen] = useState(false);
  const [pickedDate, setPickedDate] = useState(() => nowParts().date);
  const [time, setTime] = useState('');
  const [zone, setZone] = useState<Zone>('UTC');
  const [timeTouched, setTimeTouched] = useState(false);
  const [textTouched, setTextTouched] = useState(false);

  const triggerRef = useRef<HTMLDivElement>(null);
  const popperRef = useRef<HTMLDivElement>(null);

  // Applied value changed from outside (reset, URL): sync during render
  if (value !== prevValue) {
    setPrevValue(value);
    setText(value ?? '');
  }

  // Close on outside click or Escape
  useEffect(() => {
    if (!isOpen) return;
    const onClick = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        !triggerRef.current?.contains(target) &&
        !popperRef.current?.contains(target)
      ) {
        setIsOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [isOpen]);

  const openPicker = () => {
    const parts = parseTimestamp(text) ?? nowParts();
    setTimeTouched(false);
    setPickedDate(parts.date);
    setTime(parts.time);
    setZone(parts.zone);
    setIsOpen(true);
  };

  const result = buildTimestamp(pickedDate, time, zone);
  const textInvalid = textTouched && text !== '' && !parseTimestamp(text);

  const confirm = () => {
    if (!result) return;
    setText(result);
    setTextTouched(false);
    onCommit(result);
    setIsOpen(false);
  };

  const commitText = () => {
    if (text.trim() === '') onCommit(undefined);
    else if (!textInvalid) onCommit(text.trim());
  };

  const trigger = (
    <div ref={triggerRef}>
      <InputGroup>
        <Flex>
          <FlexItem spacer={{ default: 'spacerNone' }}>
            <InputGroupItem>
              <Button
                variant="control"
                aria-label="Open timestamp picker"
                icon={<OutlinedClockIcon />}
                onClick={() => (isOpen ? setIsOpen(false) : openPicker())}
              />
            </InputGroupItem>
          </FlexItem>
          <FlexItem spacer={{ default: 'spacerNone' }}>
            <InputGroupItem isFill>
              <TextInput
                value={text}
                validated={
                  textTouched && text !== '' && !parseTimestamp(text)
                    ? 'error'
                    : 'default'
                }
                placeholder="yyyy-MM-ddTHH:mm:ss.SSS±HH:mm"
                aria-label="Timestamp"
                onChange={(_, next) => {
                  setText(next);
                  setTextTouched(false);
                }}
                onBlur={() => setTextTouched(true)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    setTextTouched(true);
                    commitText();
                  }
                }}
              />
            </InputGroupItem>
          </FlexItem>
        </Flex>
      </InputGroup>
    </div>
  );

  const popper = (
    <div ref={popperRef}>
      <Panel variant="raised">
        <PanelMain>
          <PanelMainBody>
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
                  onClick={() => setIsZoneOpen((o) => !o)}
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

            <CalendarMonth
              date={pickedDate}
              onChange={(_, date) => setPickedDate(date)}
            />

            <label htmlFor="timestamp-time">Time (optional)</label>
            <TextInput
              id="timestamp-time"
              value={time}
              validated={
                timeTouched && time !== '' && !normalizeTime(time)
                  ? 'error'
                  : 'default'
              }
              placeholder="HH:mm:ss.SSS"
              onChange={(_, next) => {
                setTime(next);
                setTimeTouched(false);
              }}
              onBlur={() => setTimeTouched(true)}
            />
            <div style={{ marginTop: 'var(--pf-t--global--spacer--md)' }}>
              <Button
                variant="secondary"
                isDisabled={!result}
                onClick={confirm}
              >
                Confirm
              </Button>{' '}
              <Button variant="link" onClick={() => setIsOpen(false)}>
                Cancel
              </Button>
            </div>
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
