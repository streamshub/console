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

interface NewFilterProps {
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

export function NewFilter({
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
  offset,
  onOffsetChange,
}: NewFilterProps) {
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
                  value={offset?.toString() ?? ''}
                  onChange={(_, value) =>
                    onOffsetChange(value ? parseInt(value, 10) : undefined)
                  }
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
