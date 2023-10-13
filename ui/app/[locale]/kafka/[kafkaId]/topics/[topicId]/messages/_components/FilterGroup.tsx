import {
  Dropdown,
  DropdownItem,
  InputGroup,
  TextInput,
} from "@/libs/patternfly/react-core";
import {
  Button,
  Divider,
  InputGroupItem,
  MenuToggle,
} from "@patternfly/react-core";
import { CheckIcon, TimesIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { DateTimePicker } from "./DateTimePicker";

type Category = "offset" | "timestamp" | "epoch" | "latest";
export type FilterGroupProps = {
  isDisabled: boolean;
  offset: number | undefined;
  epoch: number | undefined;
  timestamp: string | undefined;
  onOffsetChange: (value: number | undefined) => void;
  onTimestampChange: (value: string | undefined) => void;
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
  const [value, setValue] = useState<string | undefined>();
  const [currentCategory, setCurrentCategory] = useState<Category>(
    offset ? "offset" : timestamp ? "timestamp" : epoch ? "epoch" : "latest",
  );
  const [isOpen, setIsOpen] = useState(false);
  const labels: { [key in Category]: string } = {
    offset: t("filter.offset"),
    timestamp: t("filter.timestamp"),
    epoch: t("filter.epoch"),
    latest: t("filter.latest"),
  };

  function onConfirmOffset(value: string) {
    if (value !== "") {
      const newOffset = parseInt(value, 10);
      if (Number.isInteger(newOffset)) {
        onOffsetChange(newOffset);
      }
    } else {
      onOffsetChange(undefined);
    }
  }

  function onConfirmTimestamp(value: string) {
    if (value !== "") onTimestampChange(value);
    else onTimestampChange(undefined);
  }

  function onConfirmEpoch(value: string) {
    if (value !== "" && Number(value) >= 0) onEpochChange(Number(value));
    else onEpochChange(undefined);
  }

  function onConfirm(value: string | undefined) {
    if (value === undefined) {
      return;
    }
    if (value === "") {
      setCurrentCategory("latest");
    }
    switch (currentCategory) {
      case "offset":
        onConfirmOffset(value);
        return;
      case "timestamp":
        onConfirmTimestamp(value);
        return;
      case "epoch":
        onConfirmEpoch(value);
        return;
      default:
        onLatest();
    }
  }

  return (
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
        <DropdownItem
          key="timestamp"
          value="timestamp"
          autoFocus={currentCategory === "timestamp"}
          onClick={() => setCurrentCategory("timestamp")}
        >
          {labels["timestamp"]}
        </DropdownItem>
        <DropdownItem
          key="epoch"
          value="epoch"
          autoFocus={currentCategory === "epoch"}
          onClick={() => setCurrentCategory("epoch")}
        >
          {labels["epoch"]}
        </DropdownItem>
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
      </Dropdown>
      {currentCategory === "offset" && (
        <TextInput
          isDisabled={isDisabled}
          type={"number"}
          aria-label={t("filter.offset_aria_label")}
          placeholder={t("filter.offset_placeholder")}
          onChange={(_, value) => setValue(value)}
          value={value}
          defaultValue={offset}
        />
      )}
      {currentCategory === "timestamp" && (
        <DateTimePicker
          isDisabled={isDisabled}
          value={value || timestamp}
          onChange={(value) => setValue(value.toString())}
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
          onChange={(_, value) => setValue(value)}
          value={value}
          defaultValue={epoch}
        />
      )}
      {currentCategory !== "latest" && (
        <>
          <InputGroupItem>
            <Button
              id="cancel"
              variant="control"
              onClick={() => {
                setValue("");
                onConfirm("");
              }}
            >
              <TimesIcon />
            </Button>
          </InputGroupItem>
          <InputGroupItem>
            <Button
              id="confirm"
              variant="control"
              onClick={() => onConfirm(value)}
              isDisabled={value === undefined || value === ""}
            >
              <CheckIcon />
            </Button>
          </InputGroupItem>
        </>
      )}
    </InputGroup>
  );
}
