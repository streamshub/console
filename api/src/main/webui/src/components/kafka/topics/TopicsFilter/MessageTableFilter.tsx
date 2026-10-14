import {
  Flex,
  FlexItem,
  TextInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from '@patternfly/react-core';
import { PartitionSelector } from './PartitionSelector';
import { FetchFromSelector, FromOptions } from './FetchFromSelector';
import { LimitSelect } from './LimitSelector';
import { WhereOption, WhereSelector } from './whereSelector';
import { useTranslation } from 'react-i18next';

interface MessageTableFilterProps {
  partitions: number[];
  selectedPartition?: number;
  onPartitionChange: (partition?: number) => void;
  selectedFrom: FromOptions;
  onFromChange: (from: FromOptions) => void;
  limit: number;
  onLimitChange: (limit: number) => void;
  query: string;
  onQueryChange: (query: string) => void;
  where: WhereOption;
  onWhereChange: (where: WhereOption) => void;
  offset?: number;
  onOffsetChange: (offset?: number) => void;
  offsetInput: string;
  onOffsetInputChange: (value: string) => void;
  onOffsetCommit: () => void;
  onQueryCommit: () => void;
}

function FilterSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <ToolbarItem>
      <Flex direction={{ default: 'column' }}>
        <FlexItem>{title}</FlexItem>
        <FlexItem>{children}</FlexItem>
      </Flex>
    </ToolbarItem>
  );
}

export function MessageTableFilter({
  partitions,
  selectedPartition,
  onPartitionChange,
  selectedFrom,
  onFromChange,
  limit,
  onLimitChange,
  query,
  onQueryChange,
  where,
  onWhereChange,
  offsetInput,
  onOffsetInputChange,
  onOffsetCommit,
  onQueryCommit,
}: MessageTableFilterProps) {
  const showSearch = selectedFrom !== 'latest';
  const { t } = useTranslation();

  return (
    <Toolbar>
      <ToolbarContent alignItems="start">
        <FilterSection title="Partition">
          <PartitionSelector
            partitions={partitions}
            selectedPartition={selectedPartition}
            onPartitionChange={onPartitionChange}
          />
        </FilterSection>
        <ToolbarItem variant="separator" />
        <FilterSection title="fetch form">
          <Flex>
            <FlexItem>
              <FetchFromSelector
                selectedFrom={selectedFrom}
                onFromChange={onFromChange}
              />
            </FlexItem>
            {selectedFrom === 'offset' && (
              <FlexItem>
                <TextInput
                  type="number"
                  value={offsetInput}
                  onChange={(_, value) => onOffsetInputChange(value)}
                  onBlur={onOffsetCommit}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      onOffsetCommit();
                    }
                  }}
                  placeholder={t('topics.messages.filter.offsetPlaceholder')}
                  aria-label={t('topics.messages.filter.offsetAriaLabel')}
                />
              </FlexItem>
            )}
          </Flex>
        </FilterSection>
        <FilterSection title="Limit">
          <LimitSelect value={limit} onChange={onLimitChange} />
        </FilterSection>
        {showSearch && (
          <>
            <FilterSection title="Search">
              <Flex>
                <FlexItem spacer={{ default: 'spacerSm' }}>
                  <WhereSelector where={where} onWhereChange={onWhereChange} />
                </FlexItem>
                <FlexItem>
                  <TextInput
                    type="text"
                    aria-label={t('topics.messages.advancedSearch.queryHelper')}
                    value={query}
                    onChange={(_, value) => onQueryChange(value)}
                    placeholder={t(
                      'topics.messages.advancedSearch.queryHelper'
                    )}
                    onBlur={onQueryCommit}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        onQueryCommit();
                      }
                    }}
                    style={{ width: '260px' }}
                  />
                </FlexItem>
              </Flex>
            </FilterSection>
          </>
        )}
      </ToolbarContent>
    </Toolbar>
  );
}
