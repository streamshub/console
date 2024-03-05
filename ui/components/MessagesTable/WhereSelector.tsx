import {
  Dropdown,
  DropdownItem,
  Flex,
  FlexItem,
  MenuToggle,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { MessageBrowserProps } from "./MessagesTable";

export function WhereSelector({
  value,
  onChange,
}: {
  value: MessageBrowserProps["filterWhere"];
  onChange: (value: MessageBrowserProps["filterWhere"]) => void;
}) {
  const [isJqFilter, filter] = (() => {
    if (value?.indexOf("jq:") === 0) {
      const [_, filter] = value.split("jq:");
      return [true, filter];
    }
    return [false, undefined];
  })();
  const [isOpen, setIsOpen] = useState(false);
  useEffect(() => {
    setIsOpen(false);
  }, [value]);
  return (
    <Flex>
      <FlexItem>
        <Dropdown
          data-testid={"filter-group-dropdown"}
          toggle={(toggleRef) => (
            <MenuToggle
              onClick={() => {
                setIsOpen(true);
              }}
              isDisabled={false}
              isExpanded={isOpen}
              data-testid={"filter-group"}
              ref={toggleRef}
            >
              {(() => {
                switch (value) {
                  case "value":
                    return "Value";
                  case "key":
                    return "Key";
                  case "headers":
                    return "Headers";
                  default:
                    if (isJqFilter) {
                      return "jq filter";
                    }
                    return "Anywhere";
                }
              })()}
            </MenuToggle>
          )}
          isOpen={isOpen}
          onOpenChange={() => {
            setIsOpen((v) => !v);
          }}
        >
          <DropdownItem
            isSelected={value === undefined}
            onClick={() => onChange(undefined)}
          >
            Anywhere
          </DropdownItem>
          <DropdownItem key="key" value="key" onClick={() => onChange("key")}>
            Key
          </DropdownItem>
          <DropdownItem
            isSelected={value === "headers"}
            onClick={() => onChange("headers")}
          >
            Header
          </DropdownItem>
          <DropdownItem
            isSelected={value === "value"}
            onClick={() => onChange("value")}
          >
            Value
          </DropdownItem>
          {/*<DropdownItem isSelected={isJqFilter} onClick={() => onChange("jq:")}>*/}
          {/*  jq filter*/}
          {/*</DropdownItem>*/}
        </Dropdown>
      </FlexItem>
      {isJqFilter && (
        <FlexItem>
          <TextInput value={filter} onChange={(_, v) => onChange(`jq:${v}`)} />
        </FlexItem>
      )}
    </Flex>
  );
}
