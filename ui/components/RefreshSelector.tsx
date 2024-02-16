import { DateTime } from "@/components/DateTime";
import { RefreshButton } from "@/components/refreshButton";
import {
  Button,
  Flex,
  FlexItem,
  MenuToggle,
  Select,
  SelectGroup,
  SelectList,
  SelectOption,
  Text,
  TextContent,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { PauseIcon, PlayIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { useState } from "react";

const intervals = [1, 2, 5, 10, 30, 60] as const;
export type RefreshInterval = (typeof intervals)[number];

export type RefreshSelectorProps = {
  isRefreshing: boolean;
  isLive: boolean;
  refreshInterval: RefreshInterval;
  lastRefresh: Date | undefined;
  onRefresh: () => void;
  onToggleLive: (enable: boolean) => void;
  onIntervalChange: (interval: RefreshInterval) => void;
};

export function RefreshSelector({
  isRefreshing,
  isLive,
  refreshInterval,
  lastRefresh,
  onRefresh,
  onToggleLive,
  onIntervalChange,
}: RefreshSelectorProps) {
  const t = useTranslations("RefreshButton");
  const [isOpen, setIsOpen] = useState(false);
  const toggleOpen = () => setIsOpen((o) => !o);

  return (
    <Flex direction={{ default: "column" }}>
      <Flex justifyContent={{ default: "justifyContentFlexEnd" }}>
        <FlexItem>
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
                variant={"default"}
                splitButtonOptions={{
                  variant: "action",
                  items: [
                    <Tooltip
                      content={"Automatically update the content of the page"}
                      key={"live-toggle"}
                      position={"left"}
                    >
                      <Button
                        variant={"plain"}
                        icon={isLive ? <PauseIcon /> : <PlayIcon />}
                        onClick={() => onToggleLive(!isLive)}
                      />
                    </Tooltip>,
                  ],
                }}
              />
            )}
          >
            <SelectList>
              <SelectGroup label={"Refresh rate"}>
                {intervals.map((value, idx) => (
                  <SelectOption
                    key={idx}
                    value={value}
                    onClick={() => onIntervalChange(value)}
                  >
                    {t("refresh_interval", { value })}
                  </SelectOption>
                ))}
              </SelectGroup>
            </SelectList>
          </Select>
        </FlexItem>
        <FlexItem>
          <RefreshButton
            key={"refresh"}
            onClick={onRefresh}
            isRefreshing={isRefreshing}
            isDisabled={isRefreshing || isLive}
          />
        </FlexItem>
      </Flex>
      <FlexItem>
        {lastRefresh && (
          <TextContent>
            <Text component={"small"}>
              Last update:{" "}
              <DateTime
                value={lastRefresh}
                dateStyle={"short"}
                timeStyle={"medium"}
              />
            </Text>
          </TextContent>
        )}
      </FlexItem>
    </Flex>
  );
}
