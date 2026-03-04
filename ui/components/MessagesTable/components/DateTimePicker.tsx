import type { TimePickerProps } from "@/libs/patternfly/react-core";
import {
  DatePicker,
  DatePickerProps,
  InputGroup,
  InputGroupItem,
  TimePicker,
  yyyyMMddFormat,
} from "@/libs/patternfly/react-core";
import {
  format,
  formatISO,
  getHours,
  getMinutes,
  getSeconds,
  isValid,
  parseISO,
  setHours,
  setMinutes,
  setSeconds,
} from "date-fns";
import { useEffect, useState } from "react";

export type DateTimePickerProps = {
  value: string | undefined;
  onChange: (value: string) => void;
};

export function DateTimePicker({ value, onChange }: DateTimePickerProps) {
  const [date, setDate] = useState<Date | undefined>(
    value ? parseISO(value) : undefined,
  );

  useEffect(() => {
    if (value) {
      const parsed = parseISO(value);
      if (isValid(parsed)) {
        setDate(parsed);
      }
    } else {
      setDate(undefined);
    }
  }, [value]);

  const updateParent = (newDate: Date | undefined) => {
    if (newDate && isValid(newDate)) {
      onChange(formatISO(newDate));
    }
  };

  const onDateChange: DatePickerProps["onChange"] = (_, __, newDate) => {
    if (!newDate || !isValid(newDate)) return;

    let updated = newDate;

    if (date) {
      updated = setHours(updated, getHours(date));
      updated = setMinutes(updated, getMinutes(date));
      updated = setSeconds(updated, getSeconds(date));
    }

    setDate(updated);
    updateParent(updated);
  };

  const onTimeChange: TimePickerProps["onChange"] = (
    _,
    __,
    hour,
    minutes,
    seconds,
    isValidTime,
  ) => {
    if (!isValidTime || !date) return;

    let updated = date;

    if (hour !== undefined) updated = setHours(updated, hour);
    if (minutes !== undefined) updated = setMinutes(updated, minutes);
    if (seconds !== undefined) updated = setSeconds(updated, seconds);

    setDate(updated);
    updateParent(updated);
  };

  const datePart = date && isValid(date) ? yyyyMMddFormat(date) : undefined;

  const timePart = date && isValid(date) ? format(date, "HH:mm:ss") : undefined;

  return (
    <InputGroup>
      <InputGroupItem>
        <DatePicker value={datePart} onChange={onDateChange} />
      </InputGroupItem>
      <InputGroupItem>
        <TimePicker
          time={timePart}
          isDisabled={!date}
          placeholder="HH:mm:ss"
          includeSeconds
          is24Hour
          onChange={onTimeChange}
        />
      </InputGroupItem>
    </InputGroup>
  );
}
