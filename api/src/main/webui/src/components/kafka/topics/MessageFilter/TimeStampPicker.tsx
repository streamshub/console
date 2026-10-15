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
import {
  buildTimestamp,
  defaultDraft,
  Draft,
  normalizeTimestamp,
  parseTime,
  stripOffset,
  toDraft,
  Zone,
} from '@/utils/timestamp';

interface TimestampPickerProps {
  value?: string;
  onCommit: (value: string | undefined) => void;
}

export function TimestampPicker({ value, onCommit }: TimestampPickerProps) {
  const [text, setText] = useState(value ?? '');
  const [prevValue, setPrevValue] = useState(value);

  const [draft, setDraft] = useState<Draft>(defaultDraft);

  const [isOpen, setIsOpen] = useState(false);
  const [isZoneOpen, setIsZoneOpen] = useState(false);

  const [textError, setTextError] = useState(false);
  const [timeError, setTimeError] = useState(false);

  const triggerRef = useRef<HTMLDivElement>(null);
  const popperRef = useRef<HTMLDivElement>(null);

  if (value !== prevValue) {
    setPrevValue(value);
    setText(value ?? '');
  }

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

  const patchDraft = (patch: Partial<Draft>) =>
    setDraft((d) => ({ ...d, ...patch }));

  const commit = (next: string | undefined) => {
    setText(next ?? '');
    onCommit(next);
  };

  const openPicker = () => {
    setDraft(toDraft(text) ?? defaultDraft());
    setTimeError(false);
    setIsOpen(true);
  };

  const changeZone = (zone: Zone) => {
    patchDraft({
      zone,
      time: zone === 'UTC' ? stripOffset(draft.time) : draft.time,
    });
    setTimeError(false);
  };

  const commitText = () => {
    const trimmed = text.trim();
    if (trimmed === '') return commit(undefined);

    const normalized = normalizeTimestamp(trimmed);
    if (normalized) commit(normalized);
    else setTextError(true);
  };

  const result = buildTimestamp(draft);

  const confirm = () => {
    if (!result) return;
    commit(result);
    setIsOpen(false);
  };

  const normalizeTimeField = () => {
    const parsed = parseTime(draft.time, draft.zone);
    setTimeError(!parsed);
    if (parsed) patchDraft({ time: `${parsed.time}${parsed.offset ?? ''}` });
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
                validated={textError ? 'error' : 'default'}
                placeholder="yyyy-MM-ddTHH:mm:ss.SSS±HH:mm"
                aria-label="Timestamp"
                onChange={(_, next) => {
                  setText(next);
                  setTextError(false);
                }}
                onBlur={() =>
                  setTextError(text.trim() !== '' && !normalizeTimestamp(text))
                }
                onKeyDown={(e) => e.key === 'Enter' && commitText()}
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
              selected={draft.zone}
              onSelect={(_, selection) => {
                changeZone(selection as Zone);
                setIsZoneOpen(false);
              }}
              toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                <MenuToggle
                  ref={toggleRef}
                  isFullWidth
                  isExpanded={isZoneOpen}
                  onClick={() => setIsZoneOpen((o) => !o)}
                >
                  {draft.zone === 'UTC' ? 'UTC' : 'Local time'}
                </MenuToggle>
              )}
            >
              <SelectList>
                <SelectOption value="UTC">UTC</SelectOption>
                <SelectOption value="local">Local time</SelectOption>
              </SelectList>
            </Select>

            <CalendarMonth
              date={draft.date}
              onChange={(_, date) => patchDraft({ date })}
            />

            <Flex direction={{ default: 'column' }}>
              <FlexItem>
                <label htmlFor="timestamp-time">Time (optional)</label>
              </FlexItem>
              <FlexItem>
                <TextInput
                  id="timestamp-time"
                  value={draft.time}
                  validated={timeError ? 'error' : 'default'}
                  placeholder={
                    draft.zone === 'local'
                      ? 'HH:mm:ss.SSS±HH:mm'
                      : 'HH:mm:ss.SSS'
                  }
                  onChange={(_, time) => {
                    patchDraft({ time });
                    setTimeError(false);
                  }}
                  onBlur={normalizeTimeField}
                />
              </FlexItem>
            </Flex>

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
