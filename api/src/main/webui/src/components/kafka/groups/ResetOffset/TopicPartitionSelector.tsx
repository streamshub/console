/**
 * Topic and Partition Selector Component
 * Allows selection of topics and partitions for offset reset
 */

import { useTranslation } from 'react-i18next';
import {
  FormGroup,
  Radio,
  Menu,
  MenuList,
  MenuContent,
  MenuSearch,
  MenuSearchInput,
  SearchInput,
  SelectOption,
  MenuToggle,
  MenuContainer,
} from '@patternfly/react-core';
import { useState, useRef } from 'react';
import { TopicSelection, PartitionSelection } from '@/api/types';

interface TopicPartitionSelectorProps {
  topics: Array<{ id: string; name: string }>;
  partitions: number[];
  topicSelection: TopicSelection;
  partitionSelection: PartitionSelection;
  selectedTopicId?: string;
  selectedPartition?: number;
  onTopicSelectionChange: (selection: TopicSelection) => void;
  onPartitionSelectionChange: (selection: PartitionSelection) => void;
  onTopicChange: (topicId: string) => void;
  onPartitionChange: (partition: number) => void;
}

export function TopicPartitionSelector({
  topics,
  partitions,
  topicSelection,
  partitionSelection,
  selectedTopicId,
  selectedPartition,
  onTopicSelectionChange,
  onPartitionSelectionChange,
  onTopicChange,
  onPartitionChange,
}: TopicPartitionSelectorProps) {
  const { t } = useTranslation();
  const [isTopicSelectOpen, setIsTopicSelectOpen] = useState(false);
  const [isPartitionSelectOpen, setIsPartitionSelectOpen] = useState(false);
  const [topicFilter, setTopicFilter] = useState('');

  const topicToggleRef = useRef<HTMLButtonElement | null>(null);
  const topicMenuRef = useRef<HTMLDivElement | null>(null);
  const partitionToggleRef = useRef<HTMLButtonElement | null>(null);
  const partitionMenuRef = useRef<HTMLDivElement | null>(null);

  const selectedTopic = topics.find((t) => t.id === selectedTopicId);

  const filteredTopics = topicFilter
    ? topics.filter((topic) =>
        topic.name.toLowerCase().includes(topicFilter.toLowerCase())
      )
    : topics;

  const topicMenuItems = filteredTopics.map((topic) => (
    <SelectOption
      key={topic.id}
      itemId={topic.id}
      isSelected={selectedTopicId === topic.id}
    >
      {topic.name}
    </SelectOption>
  ));

  if (topicFilter && filteredTopics.length === 0) {
    topicMenuItems.push(
      <SelectOption isDisabled key="no-results">
        {t('common.noResultsFound')}
      </SelectOption>
    );
  }

  const partitionMenuItems = partitions.map((partition) => (
    <SelectOption
      key={partition}
      itemId={partition}
      isSelected={selectedPartition === partition}
    >
      {partition}
    </SelectOption>
  ));

  const topicToggle = (
    <MenuToggle
      ref={topicToggleRef}
      onClick={() => setIsTopicSelectOpen(!isTopicSelectOpen)}
      isExpanded={isTopicSelectOpen}
      isFullWidth
    >
      {selectedTopic?.name || t('groups.resetOffset.selectTopicPlaceholder')}
    </MenuToggle>
  );

  const topicMenu = (
    <Menu
      ref={topicMenuRef}
      onSelect={(_event, itemId) => {
        onTopicChange(itemId as string);
        setIsTopicSelectOpen(false);
        setTopicFilter('');
      }}
      activeItemId={selectedTopicId}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={topicFilter}
            onChange={(_, val) => setTopicFilter(val)}
            onClear={(evt) => {
              evt.stopPropagation();
              setTopicFilter('');
            }}
            placeholder={t('common.search')}
          />
        </MenuSearchInput>
      </MenuSearch>
      <MenuContent maxMenuHeight="200px">
        <MenuList>{topicMenuItems}</MenuList>
      </MenuContent>
    </Menu>
  );

  const partitionToggle = (
    <MenuToggle
      ref={partitionToggleRef}
      onClick={() => setIsPartitionSelectOpen(!isPartitionSelectOpen)}
      isExpanded={isPartitionSelectOpen}
      isFullWidth
    >
      {selectedPartition !== undefined
        ? selectedPartition.toString()
        : t('groups.resetOffset.selectPartitionPlaceholder')}
    </MenuToggle>
  );

  const partitionMenu = (
    <Menu
      ref={partitionMenuRef}
      onSelect={(_event, itemId) => {
        onPartitionChange(Number(itemId));
        setIsPartitionSelectOpen(false);
      }}
      activeItemId={selectedPartition}
      isScrollable
    >
      <MenuContent maxMenuHeight="200px">
        <MenuList>{partitionMenuItems}</MenuList>
      </MenuContent>
    </Menu>
  );

  return (
    <>
      <FormGroup
        label={t('groups.resetOffset.applyActionOn')}
        isInline
        role="radiogroup"
      >
        <Radio
          id="all-topics-radio"
          name="topic-selection"
          label={t('groups.resetOffset.allConsumerTopics')}
          isChecked={topicSelection === 'allTopics'}
          onChange={() => onTopicSelectionChange('allTopics')}
        />
        <Radio
          id="selected-topic-radio"
          name="topic-selection"
          label={t('groups.resetOffset.selectedTopic')}
          isChecked={topicSelection === 'selectedTopic'}
          onChange={() => onTopicSelectionChange('selectedTopic')}
        />
      </FormGroup>

      {topicSelection === 'selectedTopic' && (
        <>
          <FormGroup
            label={t('groups.resetOffset.selectTopic')}
            isRequired
            fieldId="topic-select"
          >
            <MenuContainer
              toggle={topicToggle}
              toggleRef={topicToggleRef}
              menu={topicMenu}
              menuRef={topicMenuRef}
              isOpen={isTopicSelectOpen}
              onOpenChange={setIsTopicSelectOpen}
              onOpenChangeKeys={['Escape']}
            />
          </FormGroup>

          <FormGroup
            label={t('groups.resetOffset.partitions')}
            isInline
            role="radiogroup"
          >
            <Radio
              id="all-partitions-radio"
              name="partition-selection"
              label={t('groups.resetOffset.allPartitions')}
              isChecked={partitionSelection === 'allPartitions'}
              onChange={() => onPartitionSelectionChange('allPartitions')}
            />
            <Radio
              id="selected-partition-radio"
              name="partition-selection"
              label={t('groups.resetOffset.selectedPartition')}
              isChecked={partitionSelection === 'selectedPartition'}
              onChange={() => onPartitionSelectionChange('selectedPartition')}
            />
          </FormGroup>

          {partitionSelection === 'selectedPartition' && (
            <FormGroup
              label={t('groups.resetOffset.selectPartition')}
              isRequired
              fieldId="partition-select"
            >
              <MenuContainer
                toggle={partitionToggle}
                toggleRef={partitionToggleRef}
                menu={partitionMenu}
                menuRef={partitionMenuRef}
                isOpen={isPartitionSelectOpen}
                onOpenChange={setIsPartitionSelectOpen}
                onOpenChangeKeys={['Escape']}
              />
            </FormGroup>
          )}
        </>
      )}
    </>
  );
}