"use client";
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
  getHours,
  getMinutes,
  getSeconds,
  isDate,
  isValid,
  parseISO,
  setHours,
  setMinutes,
  setSeconds,
} from "date-fns";
import { useEffect, useState } from "react";

export type DateTimePickerProps = {
  value: string | undefined;
  onChange: (value: number) => void;
};

export function DateTimePicker({ value, onChange }: DateTimePickerProps) {
  const [date, setDate] = useState<Date | string | undefined>(value);

  const onDateChange: DatePickerProps["onChange"] = (_, inputDate, newDate) => {
    setDate((date) => {
      if (newDate && yyyyMMddFormat(newDate)) {
        if (date) {
          // we already have a date set, we should preserve the time part
          newDate = setHours(newDate, getHours(date));
          newDate = setMinutes(newDate, getMinutes(date));
          newDate = setSeconds(newDate, getSeconds(date));
        }
        return newDate;
      }
      return date;
    });
    if (date) {
      setDate(date);
    }
  };
  const onTimeChange: TimePickerProps["onChange"] = (
    _,
    time,
    hour,
    minutes,
    seconds,
    isValid,
  ) => {
    setDate((date) => {
      if (
        isValid &&
        date &&
        hour != undefined &&
        hour >= 0 &&
        (time.includes("AM") || time.includes("PM"))
      ) {
        let newDate = date;
        if (hour !== undefined) {
          newDate = setHours(newDate, hour);
        }
        if (minutes !== undefined) {
          newDate = setMinutes(newDate, minutes);
        }
        if (seconds !== undefined) {
          newDate = setSeconds(newDate, seconds);
        }
        return newDate;
      }
      return date;
    });
  };

  useEffect(() => {
    if (date && isDate(date)) {
      onChange(date.getTime());
    }
  }, [date, onChange]);

  const [datePart, timePart] = (() => {
    let datePart: string | undefined;
    let timePart: string | undefined;
    if (typeof date === "string") {
      const parsedDate = parseISO(date);
      if (isValid(parsedDate)) {
        datePart = yyyyMMddFormat(parsedDate);
        if (value === date) {
          timePart = format(parsedDate, "hh:mm:ss aa");
        }
      } else {
        datePart = date;
      }
    } else if (isDate(date)) {
      if (isValid(date)) {
        datePart = yyyyMMddFormat(date);
      } else {
        datePart = date.toISOString();
      }
      datePart = isValid(date) ? yyyyMMddFormat(date) : date.toISOString();
    } else {
      datePart = date;
    }
    return [datePart, timePart];
  })();

  return (
    <InputGroup>
      <InputGroupItem>
        <DatePicker value={datePart} onChange={onDateChange} />
      </InputGroupItem>
      <InputGroupItem>
        <TimePicker
          time={timePart}
          isDisabled={!date}
          placeholder={"hh:mm:ss"}
          includeSeconds={true}
          onChange={onTimeChange}
        />
      </InputGroupItem>
    </InputGroup>
  );
}
