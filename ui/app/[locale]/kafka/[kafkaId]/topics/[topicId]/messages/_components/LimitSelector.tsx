import {
  InputGroup,
  InputGroupText,
  MenuToggle,
  Select,
  SelectOption,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";

export type LimitSelectorProps = {
  value: number;
  isDisabled: boolean;
  onChange: (value: number) => void;
};

export function LimitSelector({
  value,
  isDisabled,
  onChange,
}: LimitSelectorProps) {
  const t = useTranslations("message-browser");
  const [isOpen, setIsOpen] = useState(false);
  const toggleOpen = () => setIsOpen((o) => !o);
  const titleId = "limit-selector";

  return (
    <InputGroup>
      <InputGroupText className="pf-c-content">
        {t("limit_label")}
      </InputGroupText>
      <div>
        <span id={titleId} hidden>
          Select a state
        </span>

        <Select
          aria-label={t("per_page_aria_label")}
          selected={value !== undefined ? [t("limit", { value })] : undefined}
          isOpen={isOpen}
          onSelect={() => setIsOpen(false)}
          data-testid={"limit-selector"}
          toggle={(toggleRef) => (
            <MenuToggle
              ref={toggleRef}
              onClick={toggleOpen}
              isExpanded={isOpen}
              isDisabled={isDisabled}
              style={
                {
                  width: "200px",
                } as React.CSSProperties
              }
            >
              {value || t("per_page_aria_label")}
            </MenuToggle>
          )}
        >
          {[20, 50, 100, 200, 500, 1000].map((value, idx) => (
            <SelectOption
              key={idx}
              value={value}
              onClick={() => onChange(value)}
            >
              {t("limit", { value })}
            </SelectOption>
          ))}
        </Select>
      </div>
    </InputGroup>
  );
}
