import type { SelectProps } from "@/libs/patternfly/react-core";
import {
  InputGroup,
  InputGroupText,
  MenuToggle,
  Select,
  SelectOption,
  TextInputGroup,
} from "@/libs/patternfly/react-core";
import {
  Button,
  TextInputGroupMain,
  TextInputGroupUtilities,
} from "@patternfly/react-core";
import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { TimesIcon } from "@/libs/patternfly/react-icons";

const MAX_OPTIONS = 20;

export type PartitionSelectorProps = {
  value: number | undefined;
  partitions: number | undefined;
  isDisabled: boolean;
  onChange: (value: number | undefined) => void;
};
export function PartitionSelector({
  value,
  partitions,
  isDisabled,
  onChange,
}: PartitionSelectorProps) {
  const t = useTranslations("message-browser");
  const [isOpen, setIsOpen] = useState(false);
  const toggleOpen = () => setIsOpen((o) => !o);
  const titleId = "partition-selector";

  const handleChange = useCallback(
    (value: string) => {
      if (value !== "") {
        const valueAsNum = parseInt(value, 10);
        if (Number.isInteger(valueAsNum)) {
          onChange(valueAsNum);
        }
      }
      setIsOpen(false);
    },
    [onChange],
  );

  const allPartitions = useMemo(() => {
    return new Array(partitions).fill(0).map((_, index) => index.toString());
  }, [partitions]);

  const makeOptions = useCallback(
    (values: string[]) => {
      const options = values
        .slice(0, MAX_OPTIONS)
        .map((v) => <SelectOption key={v} value={v} />);
      const hiddenOptionsCount = values.length - options.length;
      return hiddenOptionsCount
        ? [
            ...options,
            <SelectOption
              key={"more-info"}
              isDisabled={true}
              description={t("partitions_hidden", {
                count: hiddenOptionsCount,
              })}
            />,
          ]
        : options;
    },
    [t],
  );

  const options = useMemo(() => {
    return makeOptions(allPartitions);
  }, [allPartitions, makeOptions]);

  // const handleFilter: SelectProps["onFilter"] = useCallback(
  //   (_, filter: string) => {
  //     if (filter !== "") {
  //       return makeOptions(
  //         allPartitions.filter((partition) => partition.includes(filter)),
  //       );
  //     }
  //     return options;
  //   },
  //   [allPartitions, makeOptions, options],
  // );

  return (
    <InputGroup>
      <InputGroupText className="pf-c-content">
        {t("field.partition")}
      </InputGroupText>
      <div>
        <span id={titleId} hidden>
          {t("select_partition_aria_label")}
        </span>
        <Select
          onSelect={(_, value) => handleChange(value as string)}
          // selections={value !== undefined ? [`${value}`] : undefined}
          isOpen={isOpen}
          aria-labelledby={titleId}
          data-testid={"partition-selector"}
          toggle={(toggleRef) => (
            <MenuToggle
              ref={toggleRef}
              variant="typeahead"
              onClick={toggleOpen}
              isExpanded={isOpen}
              isFullWidth
              isDisabled={isDisabled}
            >
              <TextInputGroup isPlain>
                <TextInputGroupMain
                  // value={inputValue}
                  // onClick={onToggleClick}
                  // onChange={onTextInputChange}
                  // onKeyDown={onInputKeyDown}
                  id="typeahead-select-input"
                  autoComplete="off"
                  // innerRef={textInputRef}
                  placeholder="Select a state"
                  //{...(activeItem && { 'aria-activedescendant': activeItem })}
                  role="combobox"
                  isExpanded={isOpen}
                  aria-controls="select-typeahead-listbox"
                />

                <TextInputGroupUtilities>
                  {
                    /*!!inputValue && */ <Button
                      variant="plain"
                      // onClick={() => {
                      //   setSelected('');
                      //   setInputValue('');
                      //   setFilterValue('');
                      //   textInputRef?.current?.focus();
                      // }}
                      onClick={() => onChange(undefined)}
                      aria-label="Clear input value"
                    >
                      <TimesIcon aria-hidden />
                    </Button>
                  }
                </TextInputGroupUtilities>
              </TextInputGroup>
            </MenuToggle>
          )}
        >
          {options}
        </Select>
      </div>
      <InputGroupText id={`${titleId}-input`} className="pf-c-content">
        {t("select_partition_of_count", { partitions })}
      </InputGroupText>
    </InputGroup>
  );
}
