/**
 * Filter By Topic Component
 *
 * Dropdown selector for filtering charts by specific topic.
 * Allows users to view metrics for individual topics or all topics.
 * Includes search/filter functionality and scrollable list.
 */

import { useState, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
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
  SelectGroup,
} from '@patternfly/react-core';

interface FilterByTopicProps {
  topics: Array<{ id: string; name: string; isInternal?: boolean }>;
  value: string | null; // null means "All Topics"
  onChange: (topicId: string | null) => void;
  isDisabled?: boolean;
}

export function FilterByTopic({
  topics,
  value,
  onChange,
  isDisabled = false,
}: FilterByTopicProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState('');

  const toggleRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const allTopicsLabel = t('metrics.all_topics') || 'All topics';

  // Filter topics based on search input only (parent handles internal topic filtering)
  const filteredTopics = useMemo(() => {
    if (!filter) {
      return topics;
    }
    
    return topics.filter((topic) =>
      topic.name.toLowerCase().includes(filter.toLowerCase())
    );
  }, [topics, filter]);

  const selectedTopicName = useMemo(() => {
    return topics.find((t) => t.id === value)?.name;
  }, [topics, value]);

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, itemId: string | number | undefined) => {
    if (itemId === 'all-topics') {
      onChange(null);
    } else {
      onChange(itemId as string);
    }
    setIsOpen(false);
    setFilter(''); // Clear filter on selection
  };

  const menuItems = [
    <SelectOption
      key="all-topics"
      itemId="all-topics"
      isSelected={value === null}
    >
      {allTopicsLabel}
    </SelectOption>,
    <SelectGroup label="Filter by topic" key="topic-filter-group">
      {filteredTopics.map((topic) => (
        <SelectOption
          key={topic.id}
          itemId={topic.id}
          isSelected={value === topic.id}
        >
          {topic.name}
        </SelectOption>
      ))}
    </SelectGroup>,
  ];

  if (filter && filteredTopics.length === 0) {
    menuItems.push(
      <SelectOption isDisabled key="no-results">
        {t('common:no_results_found') || 'No results found'}
      </SelectOption>
    );
  }

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setIsOpen(!isOpen)}
      isExpanded={isOpen}
      isDisabled={isDisabled || topics.length === 0}
    >
      {selectedTopicName || allTopicsLabel}
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      onSelect={onSelect}
      activeItemId={value ?? 'all-topics'}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={filter}
            onChange={(_, val) => setFilter(val)}
            onClear={(evt) => {
              evt.stopPropagation();
              setFilter('');
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
    <MenuContainer
      toggle={toggle}
      toggleRef={toggleRef}
      menu={menu}
      menuRef={menuRef}
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      onOpenChangeKeys={['Escape']}
    />
  );
}