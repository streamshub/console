import { Flex, FlexItem, ToolbarItem } from '@patternfly/react-core';
import { useTranslation } from 'react-i18next';
import { PartitionSelector } from './PartitionSelector';
import { FetchFromSelector } from './FetchFromSelector';
import { LimitSelect } from './LimitSelector';
import { WhereSelector } from './whereSelector';
import { CommitInput } from './CommitInput';
import { FilterState, isValidOffset, parseOffset } from './messageFilter';
import { TimestampPicker } from './TimeStampPicker';

interface MessageTableFilterProps {
  partitions: number[];
  value: FilterState;
  onChange: (patch: Partial<FilterState>) => void;
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
  value,
  onChange,
}: MessageTableFilterProps) {
  const { t } = useTranslation();
  const { partition, from, offset, limit, query, where, timestamp } = value;

  return (
    <>
      <FilterSection title={t('topics.messages.filter.partition')}>
        <PartitionSelector
          partitions={partitions}
          selectedPartition={partition}
          onPartitionChange={(next) => onChange({ partition: next })}
        />
      </FilterSection>
      <ToolbarItem variant="separator" />
      <FilterSection title={t('topics.messages.filter.fetchFrom')}>
        <Flex>
          <FlexItem>
            <FetchFromSelector
              selectedFrom={from}
              onFromChange={(next) => onChange({ from: next })}
            />
          </FlexItem>
          {from === 'offset' && (
            <FlexItem>
              <CommitInput
                type="number"
                min={0}
                value={offset?.toString() ?? ''}
                isValid={isValidOffset}
                onCommit={(raw) => onChange({ offset: parseOffset(raw) })}
                placeholder={t('topics.messages.filter.offsetPlaceholder')}
                aria-label={t('topics.messages.filter.offsetAriaLabel')}
              />
            </FlexItem>
          )}
          {from === 'timestamp' && (
            <FlexItem>
              <TimestampPicker
                value={timestamp}
                onCommit={(ts) => onChange({ timestamp: ts })}
              />
            </FlexItem>
          )}
        </Flex>
      </FilterSection>
      <FilterSection title={t('topics.messages.filter.limit')}>
        <LimitSelect
          value={limit}
          onChange={(next) => onChange({ limit: next })}
        />
      </FilterSection>
      {from !== 'latest' && (
        <FilterSection title={t('topics.messages.filter.search')}>
          <Flex
            flexWrap={{ default: 'nowrap' }}
            spaceItems={{ default: 'spaceItemsSm' }}
          >
            <FlexItem>
              <WhereSelector
                where={where}
                onWhereChange={(next) => onChange({ where: next })}
              />
            </FlexItem>
            <FlexItem>
              <CommitInput
                aria-label={t('topics.messages.advancedSearch.queryHelper')}
                placeholder={t('topics.messages.advancedSearch.queryHelper')}
                value={query}
                onCommit={(text) => onChange({ query: text.trim() })}
                style={{ width: '260px' }}
              />
            </FlexItem>
          </Flex>
        </FilterSection>
      )}
    </>
  );
}
