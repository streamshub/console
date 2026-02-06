"use client";
import { ManagedTopicLabel } from "@/components/ManagedTopicLabel";
import {
  Menu,
  MenuList,
  MenuContent,
  MenuSearch,
  MenuSearchInput,
  SearchInput,
  SelectOption,
  MenuToggle,
  MenuContainer,
  ToolbarItem,
  SelectGroup,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useRef, useState, useMemo } from "react";

type TopicOption = { id: string; name: string; managed?: boolean };

export function FilterByTopic({
  selectedTopic,
  topicList = [],
  disableToolbar,
  onSetSelectedTopic,
}: {
  selectedTopic: string | undefined;
  topicList: TopicOption[];
  disableToolbar: boolean;
  onSetSelectedTopic: (value: string | undefined) => void;
}) {
  const t = useTranslations("metrics");
  const allTopicsLabel = t("all_topics");

  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState("");

  const toggleRef = useRef<any>();
  const menuRef = useRef<any>();

  const filteredTopics = useMemo(() => {
    return topicList.filter((topic) =>
      topic.name.toLowerCase().includes(filter.toLowerCase()),
    );
  }, [topicList, filter]);

  const selectedTopicName = useMemo(() => {
    return topicList.find((t) => t.id === selectedTopic)?.name;
  }, [topicList, selectedTopic]);

  const onSelect = (_event: any, itemId: string | number | undefined) => {
    if (itemId === "all-topics") {
      onSetSelectedTopic(undefined);
    } else {
      onSetSelectedTopic(itemId as string);
    }
    setIsOpen(false);
  };

  const menuItems = [
    <SelectOption
      key="all-topics"
      itemId="all-topics"
      isSelected={selectedTopic === undefined}
    >
      {allTopicsLabel}
    </SelectOption>,
    <SelectGroup label="Filter by topic" key="topic-filter-group">
      {filteredTopics.map((topic) => (
        <SelectOption
          key={topic.id}
          itemId={topic.id}
          isSelected={selectedTopic === topic.id}
        >
          {topic.name}
          {(topic as any).managed === true && <ManagedTopicLabel />}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  if (filter && filteredTopics.length === 0) {
    menuItems.push(
      <SelectOption isDisabled key="no-results">
        {t("common:no_results_found")}
      </SelectOption>,
    );
  }

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setIsOpen(!isOpen)}
      isExpanded={isOpen}
      isDisabled={disableToolbar || topicList.length === 0}
      className="appserv-metrics-filterbytopic"
    >
      {selectedTopicName || allTopicsLabel}
      {topicList.find((t) => t.id === selectedTopic)?.managed && (
        <ManagedTopicLabel />
      )}
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      onSelect={onSelect}
      activeItemId={selectedTopic ?? "all-topics"}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={filter}
            onChange={(_, val) => setFilter(val)}
            onClear={(evt) => {
              evt.stopPropagation();
              setFilter("");
            }}
          />
        </MenuSearchInput>
      </MenuSearch>

      <MenuContent maxMenuHeight="200px">
        <MenuList>{menuItems}</MenuList>
      </MenuContent>
    </Menu>
  );

  return (
    <ToolbarItem>
      <MenuContainer
        toggle={toggle}
        toggleRef={toggleRef}
        menu={menu}
        menuRef={menuRef}
        isOpen={isOpen}
        onOpenChange={setIsOpen}
        onOpenChangeKeys={["Escape"]}
      />
    </ToolbarItem>
  );
}
