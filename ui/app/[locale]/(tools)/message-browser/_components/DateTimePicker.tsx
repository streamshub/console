"use client";
import type {
  DatePickerProps,
  TimePickerProps,
} from "@/libs/patternfly/react-core";
import {
  DatePicker,
  InputGroup,
  TimePicker,
} from "@/libs/patternfly/react-core";
import { formatISO, parseISO, setHours, setMinutes } from "date-fns";
import { format } from "date-fns-tz";
import { useState } from "react";

export type DateTimePickerProps = {
  isDisabled: boolean;
  value: DateIsoString | undefined;
  onChange: (value: DateIsoString) => void;
};
export function DateTimePicker({
  isDisabled,
  value,
  onChange,
}: DateTimePickerProps) {
  const date = value ? parseISO(value) : undefined;

  const [time, setTime] = useState<string | null>(null);

  const onSelectCalendar: DatePickerProps["onChange"] = (_, __, newDate) => {
    if (newDate) {
      onChange(formatISO(newDate) as DateIsoString);
    }
  };

  const onSelectTime: TimePickerProps["onChange"] = (
    _,
    time,
    hour,
    minute,
    __,
    isValid,
  ) => {
    if (
      isValid &&
      date &&
      hour != undefined &&
      hour > 0 &&
      (time.includes("AM") || time.includes("PM"))
    ) {
      let newDate = date;
      if (hour !== undefined) {
        newDate = setHours(newDate, hour);
      }
      if (minute !== undefined) {
        newDate = setMinutes(newDate, minute);
      }
      setTime(time);
      onChange(formatISO(newDate) as DateIsoString);
    }
  };

  return (
    <InputGroup>
      <DatePicker
        isDisabled={isDisabled}
        value={date ? format(date, "yyyy-MM-dd") : undefined}
        onChange={onSelectCalendar}
      />
      <TimePicker
        isDisabled={!date || isDisabled}
        time={time ? time : ""}
        onChange={onSelectTime}
      />
    </InputGroup>
  );
}
