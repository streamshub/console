import {
  Button,
  Flex,
  FlexItem,
  TextInput,
  ToolbarGroup,
  ToolbarItem,
} from '@patternfly/react-core';
import { useTranslation } from 'react-i18next';
import { PartitionSelector } from './PartitionSelector';
import { FetchFromSelector } from './FetchFromSelector';
import { LimitSelect } from './LimitSelector';
import { WhereSelector } from './whereSelector';
import {
  FilterState,
  isSearchEnabled,
  parseEpoch,
  parseOffset,
} from './messageFilter';
import { TimestampPicker } from './TimeStampPicker';
import ArrowRightIcon from '@patternfly/react-icons/dist/esm/icons/arrow-right-icon';

interface MessageTableFilterProps {
  partitions: number[];
  value: FilterState;
  /** A choice was made (dropdown, timestamp): applied straight away. */
  onChange: (patch: Partial<FilterState>) => void;
  /** Text is being typed (offset, Unix timestamp, search term): not applied yet. */
  onDraftChange: (patch: Partial<FilterState>) => void;
  /** Enter or the Submit button: apply everything and run the query. */
  onSubmit: () => void;
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

const digitsOnly = (raw: string) => raw.replace(/\D/g, '');

export function MessageTableFilter({
  partitions,
  value,
  onChange,
  onDraftChange,
  onSubmit,
}: MessageTableFilterProps) {
  const { t } = useTranslation();
  const { partition, from, offset, limit, query, where, epoch, timestamp } =
    value;

  const searchEnabled = isSearchEnabled(value);

  const submitOnEnter = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      onSubmit();
    }
  };

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
      <ToolbarGroup>
        <FilterSection title={t('topics.messages.filter.fetchFrom')}>
          <FetchFromSelector
            selectedFrom={from}
            onFromChange={(next) => onChange({ from: next })}
          />
        </FilterSection>

        {from === 'offset' && (
          <FilterSection title="Offset">
            {/* Whole numbers only: anything else is removed as it is typed */}
            <TextInput
              type="text"
              inputMode="numeric"
              value={offset?.toString() ?? ''}
              onChange={(_event, raw) =>
                onDraftChange({ offset: parseOffset(digitsOnly(raw)) })
              }
              onKeyDown={submitOnEnter}
              placeholder={t('topics.messages.filter.offsetPlaceholder')}
              aria-label={t('topics.messages.filter.offsetAriaLabel')}
            />
          </FilterSection>
        )}

        {from === 'epoch' && (
          <FilterSection title="Unix timestamp">
            <TextInput
              type="text"
              inputMode="numeric"
              value={epoch?.toString() ?? ''}
              onChange={(_event, raw) =>
                onDraftChange({ epoch: parseEpoch(digitsOnly(raw)) })
              }
              onKeyDown={submitOnEnter}
              placeholder="Enter Unix timestamp in milliseconds"
              aria-label="Unix timestamp in milliseconds"
            />
          </FilterSection>
        )}

        {from === 'timestamp' && (
          <FilterSection title="Timestamp">
            <TimestampPicker
              value={timestamp}
              onCommit={(ts) => onChange({ timestamp: ts })}
            />
          </FilterSection>
        )}
      </ToolbarGroup>
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
                isDisabled={!searchEnabled}
                onWhereChange={(next) => onChange({ where: next })}
              />
            </FlexItem>
            <FlexItem>
              <TextInput
                type="text"
                isDisabled={!searchEnabled}
                aria-label={t('topics.messages.advancedSearch.queryHelper')}
                placeholder={t('topics.messages.advancedSearch.queryHelper')}
                value={query}
                onChange={(_event, text) => onDraftChange({ query: text })}
                onKeyDown={submitOnEnter}
                style={{ width: '160px' }}
              />
            </FlexItem>
          </Flex>
        </FilterSection>
      )}

      {/* Last item of the toolbar */}
      <ToolbarItem>
        <Flex direction={{ default: 'column' }}>
          {/* Same height as the section titles, so the button lines up with the controls */}
          <FlexItem aria-hidden="true">&nbsp;</FlexItem>
          <FlexItem>
            <Button
              variant="control"
              onClick={onSubmit}
              icon={<ArrowRightIcon />}
            />
          </FlexItem>
        </Flex>
      </ToolbarItem>
    </>
  );
}
