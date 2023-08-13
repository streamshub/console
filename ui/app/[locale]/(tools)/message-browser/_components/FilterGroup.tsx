import {
  Dropdown,
  DropdownItem,
  InputGroup,
  TextInput,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { Divider, MenuToggle } from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { DateTimePicker } from "./DateTimePicker";

type Category = "offset" | "timestamp" | "epoch" | "latest";
export type FilterGroupProps = {
  isDisabled: boolean;
  offset: number | undefined;
  epoch: number | undefined;
  timestamp: DateIsoString | undefined;
  onOffsetChange: (value: number | undefined) => void;
  onTimestampChange: (value: DateIsoString | undefined) => void;
  onEpochChange: (value: number | undefined) => void;
  onLatest: () => void;
};
export function FilterGroup({
  isDisabled,
  offset,
  epoch,
  timestamp,
  onOffsetChange,
  onTimestampChange,
  onEpochChange,
  onLatest,
}: FilterGroupProps) {
  const t = useTranslations("message-browser");
  const [currentCategory, setCurrentCategory] = useState<Category>("latest");
  const [isOpen, setIsOpen] = useState(false);
  const labels: { [key in Category]: string } = {
    offset: t("filter.offset"),
    timestamp: t("filter.timestamp"),
    epoch: t("filter.epoch"),
    latest: t("filter.latest"),
  };
  return (
    <ToolbarItem>
      <InputGroup>
        <Dropdown
          data-testid={"filter-group-dropdown"}
          toggle={(toggleRef) => (
            <MenuToggle
              onClick={() => setIsOpen((v) => !v)}
              isDisabled={isDisabled}
              isExpanded={isOpen}
              data-testid={"filter-group"}
              ref={toggleRef}
            >
              {labels[currentCategory]}
            </MenuToggle>
          )}
          isOpen={isOpen}
          onSelect={() => setIsOpen(false)}
        >
          <DropdownItem
            key="offset"
            value="offset"
            autoFocus={currentCategory === "offset"}
            onClick={() => setCurrentCategory("offset")}
          >
            {labels["offset"]}
          </DropdownItem>
          ,
          <DropdownItem
            key="timestamp"
            value="timestamp"
            autoFocus={currentCategory === "timestamp"}
            onClick={() => setCurrentCategory("timestamp")}
          >
            {labels["timestamp"]}
          </DropdownItem>
          ,
          <DropdownItem
            key="epoch"
            value="epoch"
            autoFocus={currentCategory === "epoch"}
            onClick={() => setCurrentCategory("epoch")}
          >
            {labels["epoch"]}
          </DropdownItem>
          ,
          <Divider component="li" key="separator" />
          <DropdownItem
            key="latest"
            value="latest"
            autoFocus={currentCategory === "latest"}
            onClick={() => {
              setCurrentCategory("latest");
              onLatest();
            }}
          >
            {labels["latest"]}
          </DropdownItem>
          ,
        </Dropdown>
        {currentCategory === "offset" && (
          <TextInput
            isDisabled={isDisabled}
            type={"number"}
            aria-label={t("filter.offset_aria_label")}
            placeholder={t("filter.offset_placeholder")}
            onChange={(_, value) => {
              if (value !== "") {
                const newOffset = parseInt(value, 10);
                if (Number.isInteger(newOffset)) {
                  onOffsetChange(newOffset);
                }
              } else {
                onOffsetChange(undefined);
              }
            }}
            value={offset === undefined ? "" : offset}
          />
        )}
        {currentCategory === "timestamp" && (
          <DateTimePicker
            isDisabled={isDisabled}
            value={timestamp}
            onChange={onTimestampChange}
          />
        )}
        {currentCategory === "epoch" && (
          <TextInput
            isDisabled={isDisabled}
            type={"number"}
            aria-label={t("filter.epoch_aria_label")}
            placeholder={t("filter.epoch_placeholder")}
            className="pf-u-flex-basis-auto pf-u-flex-grow-0 pf-u-w-initial"
            size={t("filter.epoch_placeholder").length}
            onChange={(_, value) => {
              if (value !== "" && Number(value) >= 0)
                onEpochChange(Number(value));
              else onEpochChange(undefined);
            }}
            value={epoch == undefined ? "" : epoch}
          />
        )}
      </InputGroup>
    </ToolbarItem>
  );
}
