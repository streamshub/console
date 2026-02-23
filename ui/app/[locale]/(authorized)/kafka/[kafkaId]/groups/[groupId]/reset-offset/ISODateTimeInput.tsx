"use client";

import { useEffect, useState, useMemo } from "react";
import {
  FormGroup,
  TextInput,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Button,
} from "@/libs/patternfly/react-core";
import { parseISO, isValid } from "date-fns";
import { useTranslations } from "next-intl";
import { formatInTimeZone } from "date-fns-tz";

const ISO_REGEX = {
  UTC: /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?Z?$/,
  LOCAL: /([+-]\d{2}:\d{2})$/,
};

type TimeType = "UTC" | "LOCAL" | "INVALID";

type Props = {
  value?: string;
  onValidChange: (value: string) => void;
  onValidityChange?: (isValid: boolean) => void;
  label?: string;
};

export function ISODateTimeInput({
  value = "",
  onValidChange,
  label = "ISO date time",
  onValidityChange,
}: Props) {
  const t = useTranslations("ConsumerGroupsTable");
  const [inputValue, setInputValue] = useState(value);
  const [displayZone, setDisplayZone] = useState<"UTC" | "LOCAL">("UTC");
  const [originalOffset, setOriginalOffset] = useState<string | null>(null);

  useEffect(() => {
    setInputValue(value);
  }, [value]);

  const { timeType, hasError, parsedDate } = useMemo(() => {
    const isLocal = ISO_REGEX.LOCAL.test(inputValue);
    const isUtc = ISO_REGEX.UTC.test(inputValue);

    let type: TimeType = "INVALID";

    if (!inputValue) {
      return { timeType: null, hasError: false, parsedDate: null };
    }

    const date = parseISO(inputValue);
    const valid = isValid(date) && (isLocal || isUtc);

    if (valid) {
      type = isLocal ? "LOCAL" : "UTC";
    }

    return {
      timeType: type,
      hasError: !valid && inputValue.length > 0,
      parsedDate: valid ? date : null,
    };
  }, [inputValue]);

  useEffect(() => {
    onValidityChange?.(!hasError && timeType !== "INVALID");
  }, [hasError, timeType, onValidityChange]);

  const handleChange = (_e: unknown, val: string) => {
    setInputValue(val);

    const offsetMatch = val.match(ISO_REGEX.LOCAL);
    if (offsetMatch) {
      setOriginalOffset(offsetMatch[1]);
    }

    const date = parseISO(val);
    if (isValid(date)) {
      const formatted =
        ISO_REGEX.UTC.test(val) && !val.endsWith("Z") ? `${val}Z` : val;
      onValidChange(formatted);
    }
  };

  const handleConvert = (target: "UTC" | "LOCAL") => {
    if (hasError || !parsedDate) return;
    let converted: string;

    if (target === "UTC") {
      converted = formatInTimeZone(
        parsedDate,
        "UTC",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      );
    } else {
      const targetZone =
        originalOffset || Intl.DateTimeFormat().resolvedOptions().timeZone;

      converted = formatInTimeZone(
        parsedDate,
        targetZone,
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      );
    }

    setDisplayZone(target);
    setInputValue(converted);
    onValidChange(converted);
  };
  return (
    <FormGroup label={label}>
      <TextInput
        value={inputValue}
        type="text"
        placeholder={t("date_time_placeholder")}
        validated={hasError ? "error" : "default"}
        onChange={handleChange}
      />
      {hasError && (
        <FormHelperText>
          <HelperText>
            <HelperTextItem variant="error">
              {t("invalid_format_error")}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      )}
      <FormHelperText>
        <HelperText>
          <HelperTextItem>
            {displayZone === "UTC" ? (
              <>
                {t("time_in_UTC")}{" "}
                <Button
                  variant="link"
                  isInline
                  isDisabled={hasError || !parsedDate}
                  onClick={() => handleConvert("LOCAL")}
                >
                  {t("change_to_local")}
                </Button>
              </>
            ) : (
              <>
                {t("time_in_local")}{" "}
                <Button
                  variant="link"
                  isInline
                  isDisabled={hasError || !parsedDate}
                  onClick={() => handleConvert("UTC")}
                >
                  {t("change_to_utc")}
                </Button>
              </>
            )}
          </HelperTextItem>
        </HelperText>
      </FormHelperText>
    </FormGroup>
  );
}
