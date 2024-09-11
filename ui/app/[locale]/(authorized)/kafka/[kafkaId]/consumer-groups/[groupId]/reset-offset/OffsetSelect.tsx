import { MenuToggle, MenuToggleElement, Select, SelectList, SelectOption, SelectProps } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { OffsetValue } from "../types";

export function OffsetSelect({
  value,
  onChange
}: {
  value: OffsetValue;
  onChange: (value: OffsetValue) => void;
}) {

  const t = useTranslations("ConsumerGroupsTable");

  const [isOpen, setIsOpen] = useState<boolean>(false);

  const onToggle = () => {
    setIsOpen(!isOpen);
  };

  const offsetValueOption: { [key in OffsetValue]: string } = {
    custom: t("offset.custom"),
    latest: t("offset.latest"),
    earliest: t("offset.earliest"),
    specificDateTime: t("offset.specific_date_time")
  };

  const onSelect: SelectProps["onSelect"] = (_, selection) => {
    onChange(selection as OffsetValue);
    setIsOpen(false);
  };

  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggle}
      isExpanded={isOpen}
      style={
        {
          width: '200px'
        } as React.CSSProperties
      }
    >
      {offsetValueOption[value]}
    </MenuToggle>
  );
  const makeOptions = () => {
    return Object.entries(offsetValueOption).map(([value, label]) => (
      <SelectOption key={value} value={value}>
        {label}
      </SelectOption>
    ));
  };

  return (
    <Select
      id="offset-select"
      isOpen={isOpen}
      selected={value}
      onSelect={onSelect}
      toggle={toggle}
      onOpenChange={(isOpen) => setIsOpen(isOpen)}
      shouldFocusToggleOnSelect
    >
      <SelectList>
        {makeOptions()}
      </SelectList>
    </Select>
  )
}
