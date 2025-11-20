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
import { FilterIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { useRef, useState } from "react";

export function FilterByTopic({
  selectedTopic,
  topicList = [],
  disableToolbar,
  onSetSelectedTopic,
}: {
  selectedTopic: string | undefined;
  topicList: string[];
  disableToolbar: boolean;
  onSetSelectedTopic: (value: string | undefined) => void;
}) {
  const t = useTranslations("metrics");
  const allTopicsLabel = t("all_topics");

  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState("");

  const toggleRef = useRef<any>();
  const menuRef = useRef<any>();

  const filteredTopics = topicList.filter((topic) =>
    topic.toLowerCase().includes(filter.toLowerCase()),
  );

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
      {filteredTopics.map((topic, index) => (
        <SelectOption
          key={`topic-filter-${index + 1}`}
          itemId={topic}
          title={topic}
          isSelected={selectedTopic === topic}
        >
          {topic}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  if (filter && menuItems.length === 1) {
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
      {selectedTopic || allTopicsLabel}
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
