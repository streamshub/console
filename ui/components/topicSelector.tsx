"use client";
import { setContextTopic } from "@/api/setContextPrincipal";
import {
  Button,
  Divider,
  Dropdown,
  DropdownItem,
  MenuFooter,
  MenuSearch,
  MenuSearchInput,
  MenuToggle,
  SearchInput,
} from "@/libs/patternfly/react-core";
import { useFormatter, useNow, useTranslations } from "next-intl";
import Link from "next/link";
import { useState } from "react";

export const TopicSelector = ({
  selected,
  topics,
}: {
  selected: Topic | undefined;
  topics: Topic[];
}) => {
  const t = useTranslations("topic-manager");
  const format = useFormatter();
  const now = useNow({
    // Update every 10 seconds
    updateInterval: 1000 * 10,
  });
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [searchText, setSearchText] = useState<string>("");

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const topicToDropdownItem = (p: Topic) => {
    const description = p.lastProduced
      ? format.relativeTime(p.lastProduced, now)
      : "Never used";
    return (
      <DropdownItem
        key={p.name}
        value={p.name}
        id={p.name}
        onClick={() => {
          setIsOpen(false);
        }}
        description={description}
      >
        {p.name}
      </DropdownItem>
    );
  };

  const menuItems = topics
    .filter(
      (p) =>
        searchText === "" ||
        p.name.toLowerCase().includes(searchText.toLowerCase()),
    )
    .map(topicToDropdownItem);
  return (
    <Dropdown
      isOpen={isOpen}
      onOpenChange={(isOpen) => setIsOpen(isOpen)}
      onOpenChangeKeys={["Escape"]}
      toggle={(toggleRef) => (
        <MenuToggle
          aria-label="Toggle"
          ref={toggleRef}
          onClick={onToggleClick}
          isExpanded={isOpen}
          style={{ width: "auto" }}
        >
          {selected ? selected.name : "Select a Topic"}
        </MenuToggle>
      )}
      onSelect={(_ev, value) => {
        if (typeof value === "string") {
          void setContextTopic(value);
        }
      }}
      selected={selected?.name}
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            aria-label="Filter menu items"
            value={searchText}
            onChange={(_event, value) => setSearchText(value)}
          />
        </MenuSearchInput>
      </MenuSearch>
      <Divider />

      {menuItems}

      <MenuFooter>
        <Button
          variant={"link"}
          isInline={true}
          component={() => (
            <Link onClick={onToggleClick} href={"/topic-manager"}>
              {t("manage-button")}
            </Link>
          )}
        />
      </MenuFooter>
    </Dropdown>
  );
};
