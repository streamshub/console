import { RefreshButton } from "@/components/refreshButton";
import { MenuToggle, Select, SelectOption } from "@/libs/patternfly/react-core";
import { SelectList } from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";

const intervals = [5, 10, 30, 60] as const;
export type RefreshInterval = (typeof intervals)[number] | undefined;

export type RefreshSelectorProps = {
  isRefreshing: boolean;
  refreshInterval: RefreshInterval;
  onClick: () => void;
  onChange: (interval: RefreshInterval) => void;
};

export function RefreshSelector({
  isRefreshing,
  refreshInterval,
  onClick,
  onChange,
}: RefreshSelectorProps) {
  const t = useTranslations("RefreshButton");
  const [isOpen, setIsOpen] = useState(false);
  const toggleOpen = () => setIsOpen((o) => !o);

  return (
    <Select
      aria-label={t("refresh_button_label")}
      selected={refreshInterval ? refreshInterval : -1}
      isOpen={isOpen}
      onSelect={() => setIsOpen(false)}
      toggle={(toggleRef) => (
        <MenuToggle
          ref={toggleRef}
          onClick={toggleOpen}
          isExpanded={isOpen}
          isDisabled={isRefreshing}
          variant={"plainText"}
          splitButtonOptions={{
            variant: "action",
            items: [
              <>
                <RefreshButton
                  key={"refresh"}
                  onClick={onClick}
                  isRefreshing={isRefreshing || refreshInterval}
                  isDisabled={isRefreshing}
                />

                {refreshInterval && (
                  <>
                    {t("refresh_interval", { value: refreshInterval })}
                    &nbsp;&nbsp;&nbsp;
                  </>
                )}
              </>,
            ],
          }}
        />
      )}
    >
      <SelectList>
        <SelectOption value={-1} onClick={() => onChange(undefined)}>
          Disabled
        </SelectOption>
        {intervals.map((value, idx) => (
          <SelectOption key={idx} value={value} onClick={() => onChange(value)}>
            {t("refresh_interval", { value })}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
}
