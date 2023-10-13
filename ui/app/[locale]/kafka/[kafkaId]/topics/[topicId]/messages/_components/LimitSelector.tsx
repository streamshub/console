import { MenuToggle, Select, SelectOption } from "@/libs/patternfly/react-core";
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

  return (
    <Select
      aria-label={t("per_page_aria_label", { value })}
      selected={value}
      isOpen={isOpen}
      onSelect={() => setIsOpen(false)}
      data-testid={"limit-selector"}
      toggle={(toggleRef) => (
        <MenuToggle
          ref={toggleRef}
          onClick={toggleOpen}
          isExpanded={isOpen}
          isDisabled={isDisabled}
          variant={"plainText"}
        >
          {t.rich("per_page_label", { value })}
        </MenuToggle>
      )}
    >
      {[20, 50, 100, 200, 500, 1000].map((value, idx) => (
        <SelectOption key={idx} value={value} onClick={() => onChange(value)}>
          {value}
        </SelectOption>
      ))}
    </Select>
  );
}
