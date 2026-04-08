
# Consumer Group Reset Offset Migration Plan

## Executive Summary

This document outlines the complete migration plan for the consumer group reset-offset functionality from the old Next.js UI (`ui/`) to the new React application (`api/src/main/webui/`). The migration will maintain full feature parity while adapting to the new architecture using React 18, TanStack Query, and PatternFly React components.

**Status:** Planning Complete
**Estimated Effort:** 10-12 days
**Priority:** High (Core feature for consumer group management)

## Current Implementation Analysis

### Old UI Architecture (Next.js)
**Location:** `ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/`

**Key Components:**
- [`ResetConsumerOffset.tsx`](../../../../../../ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/ResetConsumerOffset.tsx) - Main container with business logic
- [`ResetOffset.tsx`](../../../../../../ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/ResetOffset.tsx) - Form presentation component
- [`Dryrun.tsx`](../../../../../../ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/dryrun/Dryrun.tsx) - Dry-run results display
- Supporting components: `TypeaheadSelect`, `SelectComponent`, `DryrunSelect`, `ISODateTimeInput`

**API Integration:**
- Uses server actions: [`updateConsumerGroup()`](../../../../../../ui/api/groups/actions.ts:102-130)
- Endpoint: `PATCH /api/kafkas/{kafkaId}/groups/{groupId}`
- Supports dry-run mode via `meta.dryRun` parameter

### Feature Set

1. **Target Selection**
   - All consumer topics vs. specific topic
   - All partitions vs. specific partition (when topic selected)
   - Topic typeahead search
   - Partition dropdown

2. **Offset Options**
   - **Custom offset** (requires specific topic + partition)
   - **Latest offset**
   - **Earliest offset**
   - **Specific date/time** (ISO 8601 or Unix epoch)
   - **Delete committed offsets** (requires specific topic)

3. **Date/Time Input**
   - ISO 8601 format with validation
   - Unix epoch timestamp
   - Format toggle

4. **Dry Run**
   - Preview offset changes before applying
   - Display results grouped by topic
   - Show partition and new offset values
   - Copy CLI command
   - Download results option

5. **CLI Command Generation**
   - Generates equivalent `kafka-consumer-groups` command
   - Includes all parameters (topic, partition, offset type)
   - Always includes `--dry-run` flag in preview

6. **Validation & Error Handling**
   - Custom offset range validation
   - Partition existence validation
   - DateTime format validation
   - Kafka-level errors (topic not found, etc.)
   - User-friendly error messages

7. **User Flow**
   - Access from Groups table (kebab menu or detail page)
   - Only enabled for EMPTY state groups
   - Form → Dry Run → Confirm → Success/Error feedback

## New Architecture Design

### Technology Stack
- **React 18** with hooks
- **TypeScript** for type safety
- **TanStack Query** for API state management
- **PatternFly React** for UI components
- **React Router v6** for navigation
- **i18next** for internationalization

### File Structure

```
api/src/main/webui/src/
├── api/
│   ├── hooks/
│   │   └── useGroups.ts (existing - extend with mutations)
│   └── types.ts (existing - extend with offset types)
├── components/
│   ├── GroupsTable.tsx (existing - add reset action)
│   └── ResetOffset/
│       ├── ResetOffsetForm.tsx
│       ├── ResetOffsetModal.tsx
│       ├── DryRunResults.tsx
│       ├── TopicPartitionSelector.tsx
│       ├── OffsetValueSelector.tsx
│       ├── DateTimeInput.tsx
│       └── CliCommandDisplay.tsx
├── pages/
│   ├── GroupDetailPage.tsx (existing - add reset button)
│   └── GroupsPage.tsx (existing - add table action)
├── utils/
│   ├── offsetValidation.ts
│   └── cliCommandGenerator.ts
└── i18n/
    └── messages/
        └── en.json (extend with reset-offset keys)
```

## Implementation Plan

### Phase 1: Foundation (Days 1-2)

#### 1.1 Type Definitions
**File:** `api/src/main/webui/src/api/types.ts`

```typescript
// Extend existing types
export type OffsetValue = 'custom' | 'latest' | 'earliest' | 'specificDateTime' | 'delete';
export type DateTimeFormat = 'ISO' | 'Epoch';
export type TopicSelection = 'allTopics' | 'selectedTopic';
export type PartitionSelection = 'allPartitions' | 'selectedPartition';

export interface OffsetResetRequest {
  topicId: string;
  partition?: number;
  offset: string | number | null;
  metadata?: string;
}

export interface OffsetResetResult {
  topicId?: string;
  topicName: string;
  partition: number;
  offset: number | null;
  metadata?: string;
}

export interface ResetOffsetFormState {
  topicSelection: TopicSelection;
  selectedTopicId?: string;
  selectedTopicName?: string;
  partitionSelection: PartitionSelection;
  selectedPartition?: number;
  offsetValue: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeFormat: DateTimeFormat;
}

export interface ResetOffsetError {
  type: 'CustomOffsetError' | 'PartitionError' | 'KafkaError' | 
        'SpecificDateTimeNotValidError' | 'GeneralError';
  message: string;
}
```

#### 1.2 API Client Extension
**File:** `api/src/main/webui/src/api/client.ts`

Add method for offset reset (if not using hooks directly):
```typescript
export async function updateGroupOffsets(
  kafkaId: string,
  groupId: string,
  offsets: OffsetResetRequest[],
  dryRun: boolean = false
): Promise<Group | undefined> {
  const response = await apiClient.patch(
    `/api/kafkas/${kafkaId}/groups/${encodeURIComponent(groupId)}`,
    {
      meta: { dryRun },
      data: {
        type: 'groups',
        id: groupId,
        attributes: { offsets }
      }
    }
  );
  return dryRun ? response.data : undefined;
}
```

### Phase 2: API Integration (Days 2-3)

#### 2.1 TanStack Query Hooks
**File:** `api/src/main/webui/src/api/hooks/useGroups.ts`

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';

/**
 * Hook for resetting consumer group offsets
 */
export function useResetGroupOffsets(kafkaId: string, groupId: string) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      offsets, 
      dryRun = false 
    }: { 
      offsets: OffsetResetRequest[]; 
      dryRun?: boolean;
    }) => {
      const response = await apiClient.patch(
        `/api/kafkas/${kafkaId}/groups/${encodeURIComponent(groupId)}`,
        {
          meta: { dryRun },
          data: {
            type: 'groups',
            id: groupId,
            attributes: { offsets }
          }
        }
      );
      return response;
    },
    onSuccess: (data, variables) => {
      if (!variables.dryRun) {
        // Invalidate group queries to refresh data
        queryClient.invalidateQueries({ queryKey: ['group', kafkaId, groupId] });
        queryClient.invalidateQueries({ queryKey: ['groups', kafkaId] });
      }
    }
  });
}
```

### Phase 3: Utility Functions (Days 3-4)

#### 3.1 Validation Utilities
**File:** `api/src/main/webui/src/utils/offsetValidation.ts`

```typescript
import { ResetOffsetFormState, ResetOffsetError } from '../api/types';

export function validateResetOffsetForm(
  state: ResetOffsetFormState,
  availablePartitions: number[]
): ResetOffsetError[] {
  const errors: ResetOffsetError[] = [];

  // Topic validation
  if (state.topicSelection === 'selectedTopic' && !state.selectedTopicId) {
    errors.push({
      type: 'GeneralError',
      message: 'Please select a topic'
    });
  }

  // Partition validation
  if (
    state.topicSelection === 'selectedTopic' &&
    state.partitionSelection === 'selectedPartition' &&
    state.selectedPartition === undefined
  ) {
    errors.push({
      type: 'PartitionError',
      message: 'Please select a partition'
    });
  }

  // Custom offset validation
  if (state.offsetValue === 'custom') {
    if (state.topicSelection !== 'selectedTopic' || 
        state.partitionSelection !== 'selectedPartition') {
      errors.push({
        type: 'CustomOffsetError',
        message: 'Custom offset requires specific topic and partition'
      });
    }
    if (state.customOffset === undefined || state.customOffset < 0) {
      errors.push({
        type: 'CustomOffsetError',
        message: 'Please enter a valid offset value'
      });
    }
  }

  // DateTime validation
  if (state.offsetValue === 'specificDateTime') {
    if (!state.dateTime) {
      errors.push({
        type: 'SpecificDateTimeNotValidError',
        message: 'Please enter a date and time'
      });
    } else if (state.dateTimeFormat === 'ISO') {
      // Validate ISO 8601 format
      const isoRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?([+-]\d{2}:\d{2}|Z)?$/;
      if (!isoRegex.test(state.dateTime)) {
        errors.push({
          type: 'SpecificDateTimeNotValidError',
          message: 'Invalid ISO 8601 format. Expected: yyyy-MM-ddTHH:mm:ss.SSSZ'
        });
      }
    } else if (state.dateTimeFormat === 'Epoch') {
      // Validate epoch timestamp
      const epoch = Number(state.dateTime);
      if (isNaN(epoch) || epoch < 0) {
        errors.push({
          type: 'SpecificDateTimeNotValidError',
          message: 'Invalid epoch timestamp'
        });
      }
    }
  }

  // Delete validation
  if (state.offsetValue === 'delete' && state.topicSelection !== 'selectedTopic') {
    errors.push({
      type: 'GeneralError',
      message: 'Delete offsets requires specific topic selection'
    });
  }

  return errors;
}

export function isFormValid(
  state: ResetOffsetFormState,
  availablePartitions: number[]
): boolean {
  return validateResetOffsetForm(state, availablePartitions).length === 0;
}
```

#### 3.2 CLI Command Generator
**File:** `api/src/main/webui/src/utils/cliCommandGenerator.ts`

```typescript
import { ResetOffsetFormState } from '../api/types';

export function generateCliCommand(
  groupId: string,
  state: ResetOffsetFormState
): string {
  let command = `$ kafka-consumer-groups --bootstrap-server \${bootstrap-server} --group '${groupId}' `;

  // Add operation type
  if (state.offsetValue === 'delete') {
    command += '--delete-offsets ';
  } else {
    command += '--reset-offsets ';
  }

  // Add topic/partition specification
  if (state.topicSelection === 'allTopics') {
    command += '--all-topics';
  } else {
    const partition = state.partitionSelection === 'allPartitions' 
      ? '' 
      : `:${state.selectedPartition}`;
    command += `--topic ${state.selectedTopicName}${partition}`;
  }

  // Add offset specification
  if (state.offsetValue === 'custom') {
    command += ` --to-offset ${state.customOffset}`;
  } else if (state.offsetValue === 'specificDateTime') {
    const dateTime = state.dateTimeFormat === 'Epoch'
      ? convertEpochToISO(state.dateTime!)
      : state.dateTime;
    command += ` --to-datetime ${dateTime}`;
  } else if (state.offsetValue !== 'delete') {
    command += ` --to-${state.offsetValue}`;
  }

  command += ' --dry-run';

  return command;
}

function convertEpochToISO(epoch: string): string {
  const date = new Date(parseInt(epoch, 10));
  return date.toISOString();
}
```

#### 3.3 Offset Request Generator
**File:** `api/src/main/webui/src/utils/offsetRequestGenerator.ts`

```typescript
import { ResetOffsetFormState, OffsetResetRequest } from '../api/types';

export function generateOffsetRequests(
  state: ResetOffsetFormState,
  availableOffsets: Array<{ topicId: string; partition: number }>
): OffsetResetRequest[] {
  const requests: OffsetResetRequest[] = [];
  
  // Determine the offset value to use
  const offsetValue = getOffsetValue(state);

  if (state.topicSelection === 'allTopics') {
    // Apply to all topics and partitions
    availableOffsets.forEach(({ topicId, partition }) => {
      requests.push({
        topicId,
        partition,
        offset: offsetValue
      });
    });
  } else {
    // Apply to selected topic
    const topicOffsets = availableOffsets.filter(
      o => o.topicId === state.selectedTopicId
    );

    if (state.partitionSelection === 'allPartitions') {
      // All partitions of selected topic
      topicOffsets.forEach(({ topicId, partition }) => {
        requests.push({
          topicId,
          partition,
          offset: offsetValue
        });
      });
    } else {
      // Specific partition
      requests.push({
        topicId: state.selectedTopicId!,
        partition: state.selectedPartition,
        offset: offsetValue
      });
    }
  }

  // Remove duplicates
  return Array.from(
    new Map(
      requests.map(r => [`${r.topicId}-${r.partition}`, r])
    ).values()
  );
}

function getOffsetValue(state: ResetOffsetFormState): string | number | null {
  switch (state.offsetValue) {
    case 'custom':
      return state.customOffset!;
    case 'delete':
      return null;
    case 'specificDateTime':
      if (state.dateTimeFormat === 'Epoch') {
        return convertEpochToISO(state.dateTime!);
      }
      return state.dateTime!;
    case 'earliest':
    case 'latest':
    default:
      return state.offsetValue;
  }
}

function convertEpochToISO(epoch: string): string {
  const date = new Date(parseInt(epoch, 10));
  return date.toISOString();
}
```

### Phase 4: Core Components (Days 4-7)

#### 4.1 Topic/Partition Selector Component
**File:** `api/src/main/webui/src/components/ResetOffset/TopicPartitionSelector.tsx`

```typescript
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FormGroup,
  Radio,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
  Button,
} from '@patternfly/react-core';
import { TimesIcon } from '@patternfly/react-icons';
import { TopicSelection, PartitionSelection } from '../../api/types';

interface TopicPartitionSelectorProps {
  topics: Array<{ topicId: string; topicName: string }>;
  partitions: Array<{ topicId: string; partitionNumber: number }>;
  topicSelection: TopicSelection;
  partitionSelection: PartitionSelection;
  selectedTopicId?: string;
  selectedTopicName?: string;
  selectedPartition?: number;
  onTopicSelectionChange: (selection: TopicSelection) => void;
  onPartitionSelectionChange: (selection: PartitionSelection) => void;
  onTopicChange: (topicId: string, topicName: string) => void;
  onPartitionChange: (partition: number) => void;
}

export function TopicPartitionSelector({
  topics,
  partitions,
  topicSelection,
  partitionSelection,
  selectedTopicId,
  selectedTopicName,
  selectedPartition,
  onTopicSelectionChange,
  onPartitionSelectionChange,
  onTopicChange,
  onPartitionChange,
}: TopicPartitionSelectorProps) {
  const { t } = useTranslation();
  const [isTopicSelectOpen, setIsTopicSelectOpen] = useState(false);
  const [isPartitionSelectOpen, setIsPartitionSelectOpen] = useState(false);
  const [topicSearchValue, setTopicSearchValue] = useState('');

  const filteredTopics = topics.filter(topic =>
    topic.topicName.toLowerCase().includes(topicSearchValue.toLowerCase())
  );

  const availablePartitions = partitions
    .filter(p => p.topicId === selectedTopicId)
    .map(p => p.partitionNumber)
    .sort((a, b) => a - b);

  return (
    <>
      <FormGroup
        role="radiogroup"
        isInline
        fieldId="topic-selection"
        label={t('groups.resetOffset.applyActionOn')}
      >
        <Radio
          name="topic-selection"
          id="all-topics"
          label={t('groups.resetOffset.allConsumerTopics')}
          isChecked={topicSelection === 'allTopics'}
          onChange={() => onTopicSelectionChange('allTopics')}
        />
        <Radio
          name="topic-selection"
          id="selected-topic"
          label={t('groups.resetOffset.selectedTopic')}
          isChecked={topicSelection === 'selectedTopic'}
          onChange={() => onTopicSelectionChange('selectedTopic')}
        />
      </FormGroup>

      {topicSelection === 'selectedTopic' && (
        <>
          <FormGroup label={t('groups.resetOffset.topic')}>
            <Select
              id="topic-select"
              isOpen={isTopicSelectOpen}
              selected={selectedTopicName}
              onSelect={(_event, value) => {
                const topic = topics.find(t => t.topicName === value);
                if (topic) {
                  onTopicChange(topic.topicId, topic.topicName);
                  setIsTopicSelectOpen(false);
                }
              }}
              onOpenChange={(isOpen) => setIsTopicSelectOpen(isOpen)}
              toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                <MenuToggle
                  ref={toggleRef}
                  onClick={() => setIsTopicSelectOpen(!isTopicSelectOpen)}
                  isExpanded={isTopicSelectOpen}
                  style={{ width: '100%' }}
                >
                  {selectedTopicName || t('groups.resetOffset.selectTopic')}
                </MenuToggle>
              )}
            >
              <TextInputGroup>
                <TextInputGroupMain
                  value={topicSearchValue}
                  onChange={(_event, value) => setTopicSearchValue(value)}
                  placeholder={t('common.search')}
                />
                {topicSearchValue && (
                  <TextInputGroupUtilities>
                    <Button
                      variant="plain"
                      onClick={() => setTopicSearchValue('')}
                      aria-label={t('common.clear')}
                    >
                      <TimesIcon />
                    </Button>
                  </TextInputGroupUtilities>
                )}
              </TextInputGroup>
              <SelectList>
                {filteredTopics.map((topic) => (
                  <SelectOption key={topic.topicId} value={topic.topicName}>
                    {topic.topicName}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>
          </FormGroup>

          <FormGroup
            role="radiogroup"
            isInline
            fieldId="partition-selection"
            label={t('groups.resetOffset.partitions')}
          >
            <Radio
              name="partition-selection"
              id="all-partitions"
              label={t('groups.resetOffset.allPartitions')}
              isChecked={partitionSelection === 'allPartitions'}
              onChange={() => onPartitionSelectionChange('allPartitions')}
            />
            <Radio
              name="partition-selection"
              id="selected-partition"
              label={t('groups.resetOffset.selectedPartition')}
              isChecked={partitionSelection === 'selectedPartition'}
              onChange={() => onPartitionSelectionChange('selectedPartition')}
            />
          </FormGroup>

          {partitionSelection === 'selectedPartition' && (
            <FormGroup label={t('groups.resetOffset.partition')}>
              <Select
                id="partition-select"
                isOpen={isPartitionSelectOpen}
                selected={selectedPartition?.toString()}
                onSelect={(_event, value) => {
                  onPartitionChange(Number(value));
                  setIsPartitionSelectOpen(false);
                }}
                onOpenChange={(isOpen) => setIsPartitionSelectOpen(isOpen)}
                toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                  <MenuToggle
                    ref={toggleRef}
                    onClick={() => setIsPartitionSelectOpen(!isPartitionSelectOpen)}
                    isExpanded={isPartitionSelectOpen}
                    style={{ width: '100%' }}
                  >
                    {selectedPartition !== undefined
                      ? selectedPartition.toString()
                      : t('groups.resetOffset.selectPartition')}
                  </MenuToggle>
                )}
              >
                <SelectList>
                  {availablePartitions.map((partition) => (
                    <SelectOption key={partition} value={partition.toString()}>
                      {partition}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </FormGroup>
          )}
        </>
      )}
    </>
  );
}
```

#### 4.2 Offset Value Selector Component
**File:** `api/src/main/webui/src/components/ResetOffset/OffsetValueSelector.tsx`

```typescript
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FormGroup,
  Radio,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  TextInput,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { OffsetValue, DateTimeFormat, TopicSelection, PartitionSelection } from '../../api/types';
import { DateTimeInput } from './DateTimeInput';

interface OffsetValueSelectorProps {
  offsetValue: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeFormat: DateTimeFormat;
  topicSelection: TopicSelection;
  partitionSelection: PartitionSelection;
  errors?: Record<string, string>;
  onOffsetValueChange: (value: OffsetValue) => void;
  onCustomOffsetChange: (value: number) => void;
  onDateTimeChange: (value: string) => void;
  onDateTimeFormatChange: (format: DateTimeFormat) => void;
}

export function OffsetValueSelector({
  offsetValue,
  customOffset,
  dateTime,
  dateTimeFormat,
  topicSelection,
  partitionSelection,
  errors,
  onOffsetValueChange,
  onCustomOffsetChange,
  onDateTimeChange,
  onDateTimeFormatChange,
}: OffsetValueSelectorProps) {
  const { t } = useTranslation();
  const [isOffsetSelectOpen, setIsOffsetSelectOpen] = useState(false);

  const isTopicSelected = topicSelection === 'selectedTopic';
  const isPartitionSelected = partitionSelection === 'selectedPartition';

  // Build available offset options based on selections
  const offsetOptions: Array<{ value: OffsetValue; label: string }> = [
    ...(isTopicSelected && isPartitionSelected
      ? [{ value: 'custom' as OffsetValue, label: t('groups.resetOffset.customOffset') }]
      : []),
    { value: 'earliest', label: t('groups.resetOffset.earliestOffset') },
    { value: 'latest', label: t('groups.resetOffset.latestOffset') },
    { value: 'specificDateTime', label: t('groups.resetOffset.specificDateTime') },
    ...(isTopicSelected
      ? [{ value: 'delete' as OffsetValue, label: t('groups.resetOffset.deleteOffsets') }]
      : []),
  ];

  return (
    <>
      <FormGroup label={t('groups.resetOffset.newOffset')}>
        <Select
          id="offset-value-select"
          isOpen={isOffsetSelectOpen}
          selected={offsetValue}
          onSelect={(_event, value) => {
            onOffsetValueChange(value as OffsetValue);
            setIsOffsetSelectOpen(false);
          }}
          onOpenChange={(isOpen) => setIsOffsetSelectOpen(isOpen)}
          toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
            <MenuToggle
              ref={toggleRef}
              onClick={() => setIsOffsetSelectOpen(!isOffsetSelectOpen)}
              isExpanded={isOffsetSelectOpen}
              style={{ width: '100%' }}
            >
              {offsetOptions.find(o => o.value === offsetValue)?.label || 
               t('groups.resetOffset.selectOffset')}
            </MenuToggle>
          )}
        >
          <SelectList>
            {offsetOptions.map((option) => (
              <SelectOption key={option.value} value={option.value}>
                {option.label}
              </SelectOption>
            ))}
          </SelectList>
        </Select>
      </FormGroup>

      {offsetValue === 'custom' && isTopicSelected && isPartitionSelected && (
        <FormGroup
          label={t('groups.resetOffset.customOffsetValue')}
          fieldId="custom-offset-input"
        >
          <TextInput
            id="custom-offset-input"
            type="number"
            min={0}
            value={customOffset ?? ''}
            onChange={(_event, value) => {
              const numValue = parseInt(value, 10);
              if (!isNaN(numValue) && numValue >= 0) {
                onCustomOffsetChange(numValue);
              }
            }}
          />
          {errors?.CustomOffsetError && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
                  {errors.CustomOffsetError}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
        </FormGroup>
      )}

      {offsetValue === 'specificDateTime' && (
        <>
          <FormGroup
            role="radiogroup"
            isInline
            fieldId="datetime-format"
            label={t('groups.resetOffset.selectDateTimeFormat')}
          >
            <Radio
              name="datetime-format"
              id="iso-format"
              label={`${t('groups.resetOffset.isoFormat')} (yyyy-MM-dd'T'HH:mm:ss.SSS)`}
              isChecked={dateTimeFormat === 'ISO'}
              onChange={() => onDateTimeFormatChange('ISO')}
            />
            <Radio
              name="datetime-format"
              id="epoch-format"
              label={t('groups.resetOffset.epochFormat')}
              isChecked={dateTimeFormat === 'Epoch'}
              onChange={() => onDateTimeFormatChange('Epoch')}
            />
          </FormGroup>

          <DateTimeInput
            format={dateTimeFormat}
            value={dateTime ?? ''}
            onChange={onDateTimeChange}
            error={errors?.SpecificDateTimeNotValidError}
          />
        </>
      )}
    </>
  );
}
```

#### 4.3 DateTime Input Component
**File:** `api/src/main/webui/src/components/ResetOffset/DateTimeInput.tsx`

```typescript
import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FormGroup,
  TextInput,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from '@patternfly/react-core';
import { ExclamationCircleIcon, CheckCircleIcon } from '@patternfly/react-icons';
import { DateTimeFormat } from '../../api/types';

interface DateTimeInputProps {
  format: DateTimeFormat;
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

export function DateTimeInput({ format, value, onChange, error }: DateTimeInputProps) {
  const { t } = useTranslation();
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    if (format === 'ISO') {
      const isoRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?([+-]\d{2}:\d{2}|Z)?$/;
      setIsValid(isoRegex.test(value));
    } else {
      const epoch = Number(value);
      setIsValid(!isNaN(epoch) && epoch >= 0);
    }
  }, [format, value]);

  return (
    <FormGroup
      label={format === 'ISO' 
        ? t('groups.resetOffset.isoDateTime') 
        : t('groups.resetOffset.epochTimestamp')}
      fieldId="datetime-input"
    >
      <TextInput
        id="datetime-input"
        type={format === 'Epoch' ? 'number' : 'text'}
        value={value}
        onChange={(_event, val) => onChange(val)}
        placeholder={
          format === 'ISO'
            ? '2024-01-15T10:30:00.000Z'
            : '1705318200000'
        }
        validated={error ? 'error' : isValid && value ? 'success' : 'default'}
      />
      {error ? (
        <FormHelperText>
          <HelperText>
            <HelperTextItem icon={<ExclamationCircleIcon />} variant="error">
              {error}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      ) : isValid && value ? (
        <FormHelperText>
          <HelperText>
            <HelperTextItem icon={<CheckCircleIcon />} variant="success">
              {t('groups.resetOffset.validFormat')}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      ) : (
        <FormHelperText>
          <HelperText>
            <HelperTextItem>
              {format === 'ISO'
                ? t('groups.resetOffset.isoFormatHelp')
                : t('groups.resetOffset.epochFormatHelp')}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      )}
    </FormGroup>
  );
}
```

#### 4.4 CLI Command Display Component
**File:** `api/src/main/webui/src/components/ResetOffset/CliCommandDisplay.tsx`

```typescript
import { useTranslation } from 'react-i18next';
import {
  ClipboardCopy,
  FormGroup,
} from '@patternfly/react-core';

interface CliCommandDisplayProps {
  command: string;
}

export function CliCommandDisplay({ command }: CliCommandDisplayProps) {
  const { t } = useTranslation();

  return (
    <FormGroup label={t('groups.resetOffset.cliCommand')}>
      <ClipboardCopy
        isReadOnly
        hoverTip={t('common.copy')}
        clickTip={t('common.copied')}
      >
        {command}
      </ClipboardCopy>
    </FormGroup>
  );
}
```

#### 4.5 Main Reset Offset Form Component
**File:** `api/src/main/webui/src/components/ResetOffset/ResetOffsetForm.tsx`

```typescript
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  FormSection,
  ActionGroup,
  Button,
  Alert,
  Spinner,
} from '@patternfly/react-core';
import { Group } from '../../api/types';
import { useResetGroupOffsets } from '../../api/hooks/useGroups';
import { TopicPartitionSelector } from './TopicPartitionSelector';
import { OffsetValueSelector } from './OffsetValueSelector';
import { CliCommandDisplay } from './CliCommandDisplay';
import { validateResetOffsetForm, isFormValid } from '../../utils/offsetValidation';
import { generateCliCommand } from '../../utils/cliCommandGenerator';
import { generateOffsetRequests } from '../../utils/offsetRequestGenerator';
import type { 
  ResetOffsetFormState, 
  TopicSelection, 
  PartitionSelection, 
  OffsetValue,
  DateTimeFormat 
} from '../../api/types';

interface ResetOffsetFormProps {
  kafkaId: string;
  group: Group;
  onSuccess?: () => void;
  onCancel?: () => void;
  onDryRun?: (results: any) => void;
}

export function ResetOffsetForm({
  kafkaId,
  group,
  onSuccess,
  onCancel,
  onDryRun,
}: ResetOffsetFormProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const resetMutation = useResetGroupOffsets(kafkaId, group.id);

  const [formState, setFormState] = useState<ResetOffsetFormState>({
    topicSelection: 'allTopics',
    partitionSelection: 'allPartitions',
    offsetValue: 'earliest',
    dateTimeFormat: 'ISO',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  // Extract topics and partitions from group offsets
  const topics = Array.from(
    new Map(
      (group.attributes.offsets || [])
        .filter(o => o.topicId)
        .map(o => [o.topicId, { topicId: o.topicId!, topicName: o.topicName }])
    ).values()
  );

  const partitions = (group.attributes.offsets || [])
    .filter(o => o.topicId)
    .map(o => ({ topicId: o.topicId!, partitionNumber: o.partition }));

  const availablePartitions = partitions.map(p => p.partitionNumber);

  const cliCommand = generateCliCommand(group.attributes.groupId, formState);

  const handleSubmit = async (dryRun: boolean = false) => {
    // Validate form
    const validationErrors = validateResetOffsetForm(formState, availablePartitions);
    if (validationErrors.length > 0) {
      const errorMap: Record<string, string> = {};
      validationErrors.forEach(err => {
        errorMap[err.type] = err.message;
      });
      setErrors(errorMap);
      return;
    }

    setErrors({});

    // Generate offset requests
    const offsetRequests = generateOffsetRequests(formState, partitions);

    try {
      const result = await resetMutation.mutateAsync({
        offsets: offsetRequests,
        dryRun,
      });

      if (dryRun) {
        // Show dry run results
        onDryRun?.(result.data);
      } else {
        // Success - navigate back or call success callback
        onSuccess?.();
      }
    } catch (error: any) {
      // Handle API errors
      const apiErrors = error.response?.data?.errors || [];
      const errorMap: Record<string, string> = {};
      
      apiErrors.forEach((err: any) => {
        if (err.detail.includes('must be between the earliest offset')) {
          errorMap.CustomOffsetError = err.detail;
        } else if (err.detail.includes('not valid for topic')) {
          errorMap.PartitionError = err.detail;
        } else if (err.detail.includes('does not exist') || err.status === '404') {
          errorMap.KafkaError = err.detail;
        } else if (err.detail.includes('valid UTC ISO timestamp')) {
          errorMap.SpecificDateTimeNotValidError = err.detail;
        } else {
          errorMap.GeneralError = err.detail;
        }
      });

      setErrors(errorMap);
    }
  };

  const formValid = isFormValid(formState, availablePartitions);

  return (
    <Form>
      {errors.GeneralError && (
        <Alert variant="danger" isInline title={errors.GeneralError} />
      )}
      {errors.KafkaError && (
        <Alert variant="danger" isInline title={errors.KafkaError} />
      )}

      <FormSection title={t('groups.resetOffset.target')}>
        <TopicPartitionSelector
          topics={topics}
          partitions={partitions}
          topicSelection={formState.topicSelection}
          partitionSelection={formState.partitionSelection}
          selectedTopicId={formState.selectedTopicId}
          selectedTopicName={formState.selectedTopicName}
          selectedPartition={formState.selectedPartition}
          onTopicSelectionChange={(selection: TopicSelection) =>
            setFormState(prev => ({ ...prev, topicSelection: selection }))
          }
          onPartitionSelectionChange={(selection: PartitionSelection) =>
            setFormState(prev => ({ ...prev, partitionSelection: selection }))
          }
          onTopicChange={(topicId: string, topicName: string) =>
            setFormState(prev => ({
              ...prev,
              selectedTopicId: topicId,
              selectedTopicName: topicName,
            }))
          }
          onPartitionChange={(partition: number) =>
            setFormState(prev => ({ ...prev, selectedPartition: partition }))
          }
        />
      </FormSection>

      <FormSection title={t('groups.resetOffset.offsetDetails')}>
        <OffsetValueSelector
          offsetValue={formState.offsetValue}
          customOffset={formState.customOffset}
          dateTime={formState.dateTime}
          dateTimeFormat={formState.dateTimeFormat}
          topicSelection={formState.topicSelection}
          partitionSelection={formState.partitionSelection}
          errors={errors}
          onOffsetValueChange={(value: OffsetValue) =>
            setFormState(prev => ({ ...prev, offsetValue: value }))
          }
          onCustomOffsetChange={(value: number) =>
            setFormState(prev => ({ ...prev, customOffset: value }))
          }
          onDateTimeChange={(value: string) =>
            setFormState(prev => ({ ...prev, dateTime: value }))
          }
          onDateTimeFormatChange={(format: DateTimeFormat) =>
            setFormState(prev => ({ ...prev, dateTimeFormat: format }))
          }
        />

        <CliCommandDisplay command={cliCommand} />
      </FormSection>

      <ActionGroup>
        <Button
          variant="primary"
          onClick={() => handleSubmit(false)}
          isDisabled={!formValid || resetMutation.isPending}
        >
          {resetMutation.isPending ? <Spinner size="sm" /> : t('groups.resetOffset.reset')}
        </Button>
        <Button
          variant="secondary"
          onClick={() => handleSubmit(true)}
          isDisabled={!formValid || resetMutation.isPending}
        >
          {t('groups.resetOffset.dryRun')}
        </Button>
        <Button
          variant="link"
          onClick={onCancel}
          isDisabled={resetMutation.isPending}
        >
          {t('common.cancel')}
        </Button>
      </ActionGroup>
    </Form>
  );
}
```

#### 4.6 Dry Run Results Component
**File:** `api/src/main/webui/src/components/ResetOffset/DryRunResults.tsx`

```typescript
import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Flex,
  FlexItem,
  List,
  ListItem,
  Button,
  Alert,
  JumpLinks,
  JumpLinksItem,
  Sidebar,
  SidebarPanel,
  SidebarContent,
} from '@patternfly/react-core';
import { OffsetResetResult } from '../../api/types';

interface DryRunResultsProps {
  results: OffsetResetResult[];
  cliCommand: string;
  onBack: () => void;
  onDownload?: () => void;
}

export function DryRunResults({
  results,
  cliCommand,
  onBack,
  onDownload,
}: DryRunResultsProps) {
  const { t } = useTranslation();

  // Group results by topic
  const groupedResults = results.reduce<Record<string, OffsetResetResult[]>>(
    (acc, result) => {
      if (!acc[result.topicName]) {
        acc[result.topicName] = [];
      }
      acc[result.topicName].push(result);
      return acc;
    },
    {}
  );

  const topicNames = Object.keys(groupedResults);
  const hasResults = results.length > 0;

  return (
    <>
      {!hasResults ? (
        <Alert variant="warning" isInline title={t('groups.resetOffset.noOffsetsFound')}>
          {t('groups.resetOffset.noOffsetsFoundDescription')}
        </Alert>
      ) : (
        <Sidebar>
          {topicNames.length >= 3 && (
            <SidebarPanel>
              <JumpLinks
                isVertical
                label={t('groups.resetOffset.jumpToTopic')}
                offset={10}
              >
                {topicNames.map((topicName) => (
                  <JumpLinksItem key={topicName} href={`#${topicName}`}>
                    {topicName}
                  </JumpLinksItem>
                ))}
              </JumpLinks>
            </SidebarPanel>
          )}
          <SidebarContent style={{ overflowY: 'auto', maxHeight: '500px' }}>
            <Flex direction={{ default: 'column' }} spaceItems={{ default: 'spaceItemsXl' }}>
              {Object.entries(groupedResults).map(([topicName, offsets]) => (
                <FlexItem key={topicName}>
                  <Card component="div">
                    <CardBody>
                      <DescriptionList id={topicName}>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t('groups.resetOffset.topic')}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {topicName}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <Flex>
                          <FlexItem>
                            <DescriptionListGroup>
                              <DescriptionListTerm>
                                {t('groups.resetOffset.partition')}
                              </DescriptionListTerm>
                              <DescriptionListDescription>
                                <List isPlain>
                                  {offsets
                                    .sort((a, b) => a.partition - b.partition)
                                    .map(({ partition }) => (
                                      <ListItem key={partition}>{partition}</ListItem>
                                    ))}
                                </List>
                              </DescriptionListDescription>
                            </DescriptionListGroup>
                          </FlexItem>
                          <FlexItem>
                            <DescriptionListGroup>
                              <DescriptionListTerm>
                                {t('groups.resetOffset.newOffset')}
                              </DescriptionListTerm>
                              <DescriptionListDescription>
                                <List isPlain>
                                  {offsets.map(({ partition, offset }) => (
                                    <ListItem key={partition}>
                                      {offset ?? t('groups.resetOffset.offsetDeleted')}
                                    </ListItem>
                                  ))}
                                </List>
                              </DescriptionListDescription>
                            </DescriptionListGroup>
                          </FlexItem>
                        </Flex>
                      </DescriptionList>

                    </CardBody>
                  </Card>
                </FlexItem>
              ))}
            </Flex>
          </SidebarContent>
        </Sidebar>
      )}

      <Alert
        variant="info"
        isInline
        title={t('groups.resetOffset.dryRunExecutionAlert')}
      />

      <Flex spaceItems={{ default: 'spaceItemsSm' }}>
        <FlexItem>
          <Button variant="secondary" onClick={onBack}>
            {t('groups.resetOffset.backToEditOffset')}
          </Button>
        </FlexItem>
        {onDownload && (
          <FlexItem>
            <Button variant="link" onClick={onDownload}>
              {t('groups.resetOffset.downloadResults')}
            </Button>
          </FlexItem>
        )}
      </Flex>
    </>
  );
}
```

#### 4.7 Reset Offset Modal Component
**File:** `api/src/main/webui/src/components/ResetOffset/ResetOffsetModal.tsx`

```typescript
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
} from '@patternfly/react-core';
import { Group } from '../../api/types';
import { ResetOffsetForm } from './ResetOffsetForm';
import { DryRunResults } from './DryRunResults';

interface ResetOffsetModalProps {
  kafkaId: string;
  group: Group;
  isOpen: boolean;
  onClose: () => void;
}

export function ResetOffsetModal({
  kafkaId,
  group,
  isOpen,
  onClose,
}: ResetOffsetModalProps) {
  const { t } = useTranslation();
  const [dryRunResults, setDryRunResults] = useState<any>(null);

  const handleSuccess = () => {
    onClose();
    // Optionally show success toast
  };

  const handleDryRun = (results: any) => {
    setDryRunResults(results);
  };

  const handleBackToForm = () => {
    setDryRunResults(null);
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        dryRunResults
          ? t('groups.resetOffset.dryRunResults')
          : t('groups.resetOffset.resetConsumerOffset')
      }
      isOpen={isOpen}
      onClose={onClose}
      actions={
        dryRunResults
          ? []
          : undefined
      }
    >
      {dryRunResults ? (
        <DryRunResults
          results={dryRunResults.attributes?.offsets || []}
          cliCommand="" // Pass from form state if needed
          onBack={handleBackToForm}
        />
      ) : (
        <ResetOffsetForm
          kafkaId={kafkaId}
          group={group}
          onSuccess={handleSuccess}
          onCancel={onClose}
          onDryRun={handleDryRun}
        />
      )}
    </Modal>
  );
}
```

### Phase 5: Integration (Days 7-8)

#### 5.1 Update GroupsTable Component
**File:** `api/src/main/webui/src/components/GroupsTable.tsx`

Add reset offset action to the table:

```typescript
import { useState } from 'react';
import { ResetOffsetModal } from './ResetOffset/ResetOffsetModal';

// Inside GroupsTable component, add state for modal
const [resetOffsetGroup, setResetOffsetGroup] = useState<Group | null>(null);

// Add action menu or button in table rows
// For groups with state === 'EMPTY', enable reset offset action
{group.attributes.state === 'EMPTY' && (
  <Button
    variant="link"
    onClick={() => setResetOffsetGroup(group)}
  >
    {t('groups.resetOffset.resetOffset')}
  </Button>
)}

// Add modal at end of component
{resetOffsetGroup && (
  <ResetOffsetModal
    kafkaId={kafkaId}
    group={resetOffsetGroup}
    isOpen={!!resetOffsetGroup}
    onClose={() => setResetOffsetGroup(null)}
  />
)}
```

#### 5.2 Update Group Detail Page
**File:** `api/src/main/webui/src/pages/GroupDetailPage.tsx`

Add reset offset button to group detail page header/actions.

#### 5.3 Add Routing (if using separate pages instead of modal)
**File:** `api/src/main/webui/src/routes/index.tsx`

```typescript
{
  path: '/kafka/:kafkaId/groups/:groupId/reset-offset',
  element: <ResetOffsetPage />,
}
```

### Phase 6: Internationalization (Day 8)

#### 6.1 Add Translation Keys
**File:** `api/src/main/webui/src/i18n/messages/en.json`

```json
{
  "groups": {
    "resetOffset": {
      "resetOffset": "Reset Offset",
      "resetConsumerOffset": "Reset Consumer Offset",
      "applyActionOn": "Apply action on",
      "allConsumerTopics": "All consumer topics",
      "selectedTopic": "Selected topic",
      "topic": "Topic",
      "selectTopic": "Select topic",
      "partitions": "Partitions",
      "allPartitions": "All partitions",
      "selectedPartition": "Selected partition",
      "partition": "Partition",
      "selectPartition": "Select partition",
      "offsetDetails": "Offset details",
      "newOffset": "New offset",
      "selectOffset": "Select offset",
      "customOffset": "Custom offset",
      "customOffsetValue": "Custom offset value",
      "earliestOffset": "Earliest offset",
      "latestOffset": "Latest offset",
      "specificDateTime": "Specific date and time",
      "deleteOffsets": "Delete committed offsets",
      "selectDateTimeFormat": "Select date/time format",
      "isoFormat": "ISO 8601 format",
      "epochFormat": "Unix epoch timestamp",
      "isoDateTime": "ISO date and time",
      "epochTimestamp": "Epoch timestamp",
      "validFormat": "Valid format",
      "isoFormatHelp": "Format: yyyy-MM-dd'T'HH:mm:ss.SSSZ (e.g., 2024-01-15T10:30:00.000Z)",
      "epochFormatHelp": "Unix timestamp in milliseconds (e.g., 1705318200000)",
      "cliCommand": "CLI command",
      "target": "Target",
      "reset": "Reset",
      "dryRun": "Dry run",
      "dryRunResults": "Dry run results",
      "noOffsetsFound": "No offsets found",
      "noOffsetsFoundDescription": "No offset changes will be applied based on the current selection.",
      "jumpToTopic": "Jump to topic",
      "offsetDeleted": "Deleted",
      "dryRunExecutionAlert": "This is a dry run. No changes have been applied to the consumer group.",
      "backToEditOffset": "Back to edit offset",
      "downloadResults": "Download results",
      "resetOffsetSubmittedSuccessfully": "Offset reset submitted successfully for group {groupId}",
      "resetOffsetDescription": "Reset the consumer offsets for this group",
      "resetOffsetDescriptionNonConsumer": "Offset reset is only supported for consumer groups. This is a {protocol} group.",
      "resetOffsetDisabledNotEmpty": "Offset reset is only possible for groups in EMPTY state"
    }
  }
}
```

### Phase 7: Testing (Days 9-10)

#### 7.1 Unit Tests

**File:** `api/src/main/webui/src/utils/__tests__/offsetValidation.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { validateResetOffsetForm, isFormValid } from '../offsetValidation';
import { ResetOffsetFormState } from '../../api/types';

describe('offsetValidation', () => {
  describe('validateResetOffsetForm', () => {
    it('should validate custom offset requires specific topic and partition', () => {
      const state: ResetOffsetFormState = {
        topicSelection: 'allTopics',
        partitionSelection: 'allPartitions',
        offsetValue: 'custom',
        dateTimeFormat: 'ISO',
      };

      const errors = validateResetOffsetForm(state, [0, 1, 2]);
      expect(errors).toHaveLength(1);
      expect(errors[0].type).toBe('CustomOffsetError');
    });

    it('should validate ISO datetime format', () => {
      const state: ResetOffsetFormState = {
        topicSelection: 'allTopics',
        partitionSelection: 'allPartitions',
        offsetValue: 'specificDateTime',
        dateTime: 'invalid-date',
        dateTimeFormat: 'ISO',
      };

      const errors = validateResetOffsetForm(state, [0, 1, 2]);
      expect(errors.some(e => e.type === 'SpecificDateTimeNotValidError')).toBe(true);
    });

    it('should validate epoch timestamp', () => {
      const state: ResetOffsetFormState = {
        topicSelection: 'allTopics',
        partitionSelection: 'allPartitions',
        offsetValue: 'specificDateTime',
        dateTime: 'not-a-number',
        dateTimeFormat: 'Epoch',
      };

      const errors = validateResetOffsetForm(state, [0, 1, 2]);
      expect(errors.some(e => e.type === 'SpecificDateTimeNotValidError')).toBe(true);
    });

    it('should pass validation for valid earliest offset', () => {
      const state: ResetOffsetFormState = {
        topicSelection: 'allTopics',
        partitionSelection: 'allPartitions',
        offsetValue: 'earliest',
        dateTimeFormat: 'ISO',
      };

      const errors = validateResetOffsetForm(state, [0, 1, 2]);
      expect(errors).toHaveLength(0);
    });
  });
});
```

**File:** `api/src/main/webui/src/utils/__tests__/cliCommandGenerator.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { generateCliCommand } from '../cliCommandGenerator';
import { ResetOffsetFormState } from '../../api/types';

describe('cliCommandGenerator', () => {
  it('should generate command for all topics with earliest offset', () => {
    const state: ResetOffsetFormState = {
      topicSelection: 'allTopics',
      partitionSelection: 'allPartitions',
      offsetValue: 'earliest',
      dateTimeFormat: 'ISO',
    };

    const command = generateCliCommand('my-group', state);
    expect(command).toContain('--group \'my-group\'');
    expect(command).toContain('--all-topics');
    expect(command).toContain('--to-earliest');
    expect(command).toContain('--dry-run');
  });

  it('should generate command for specific topic and partition with custom offset', () => {
    const state: ResetOffsetFormState = {
      topicSelection: 'selectedTopic',
      selectedTopicName: 'my-topic',
      partitionSelection: 'selectedPartition',
      selectedPartition: 2,
      offsetValue: 'custom',
      customOffset: 1000,
      dateTimeFormat: 'ISO',
    };

    const command = generateCliCommand('my-group', state);
    expect(command).toContain('--topic my-topic:2');
    expect(command).toContain('--to-offset 1000');
  });

  it('should generate command for delete offsets', () => {
    const state: ResetOffsetFormState = {
      topicSelection: 'selectedTopic',
      selectedTopicName: 'my-topic',
      partitionSelection: 'allPartitions',
      offsetValue: 'delete',
      dateTimeFormat: 'ISO',
    };

    const command = generateCliCommand('my-group', state);
    expect(command).toContain('--delete-offsets');
    expect(command).toContain('--topic my-topic');
    expect(command).not.toContain('--to-');
  });
});
```

#### 7.2 Component Tests

**File:** `api/src/main/webui/src/components/ResetOffset/__tests__/ResetOffsetForm.test.tsx`

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ResetOffsetForm } from '../ResetOffsetForm';
import { Group } from '../../../api/types';

const mockGroup: Group = {
  id: 'test-group',
  type: 'groups',
  attributes: {
    groupId: 'test-group',
    state: 'EMPTY',
    type: 'Consumer',
    offsets: [
      {
        topicId: 'topic-1',
        topicName: 'test-topic',
        partition: 0,
        offset: 100,
      },
      {
        topicId: 'topic-1',
        topicName: 'test-topic',
        partition: 1,
        offset: 200,
      },
    ],
  },
};

describe('ResetOffsetForm', () => {
  const queryClient = new QueryClient();

  const renderForm = (props = {}) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <ResetOffsetForm
          kafkaId="kafka-1"
          group={mockGroup}
          {...props}
        />
      </QueryClientProvider>
    );
  };

  it('should render form with default values', () => {
    renderForm();
    
    expect(screen.getByText(/all consumer topics/i)).toBeInTheDocument();
    expect(screen.getByText(/earliest offset/i)).toBeInTheDocument();
  });

  it('should enable topic selection when radio is clicked', () => {
    renderForm();
    
    const selectedTopicRadio = screen.getByLabelText(/selected topic/i);
    fireEvent.click(selectedTopicRadio);
    
    expect(screen.getByText(/select topic/i)).toBeInTheDocument();
  });

  it('should show partition selector when topic is selected', async () => {
    renderForm();
    
    // Select specific topic
    const selectedTopicRadio = screen.getByLabelText(/selected topic/i);
    fireEvent.click(selectedTopicRadio);
    
    // Should show partition options
    await waitFor(() => {
      expect(screen.getByText(/all partitions/i)).toBeInTheDocument();
    });
  });

  it('should call onCancel when cancel button is clicked', () => {
    const onCancel = vi.fn();
    renderForm({ onCancel });
    
    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    fireEvent.click(cancelButton);
    
    expect(onCancel).toHaveBeenCalled();
  });
});
```

### Phase 8: Documentation (Days 10-11)

#### 8.1 Update User Documentation
**File:** `docs/sources/modules/proc-resetting-consumer-offsets.adoc`

Update to reflect new UI implementation (if needed).

#### 8.2 Create Developer Documentation
**File:** `api/src/main/webui/docs/RESET_OFFSET_FEATURE.md`

```markdown
# Reset Offset Feature

## Overview
The reset offset feature allows users to reset consumer group offsets to specific positions.

## Architecture

### Components
- `ResetOffsetForm` - Main form component
- `TopicPartitionSelector` - Topic and partition selection
- `OffsetValueSelector` - Offset value selection
- `DateTimeInput` - Date/time input with validation
- `DryRunResults` - Display dry run results
- `ResetOffsetModal` - Modal wrapper

### API Integration
Uses TanStack Query mutation: `useResetGroupOffsets`

### Validation
- Form validation in `utils/offsetValidation.ts`
- Real-time validation feedback
- Server-side validation errors displayed

### CLI Command Generation
Generates equivalent kafka-consumer-groups command for reference.

## Usage

### From Groups Table
1. Find group in EMPTY state
2. Click reset offset action
3. Configure offset parameters
4. Run dry run (optional)
5. Apply changes

### Supported Offset Types
- Custom offset (specific value)
- Earliest offset
- Latest offset
- Specific date/time (ISO 8601 or Unix epoch)
- Delete committed offsets

## Testing
Run tests: `npm test ResetOffset`

## Future Enhancements
- Bulk offset reset for multiple groups
- Offset reset history/audit log
- Visual offset timeline
```

### Phase 9: Final Integration & Polish (Days 11-12)

#### 9.1 Accessibility Audit
- Add ARIA labels to all form controls
- Ensure keyboard navigation works
- Test with screen readers
- Add focus management

#### 9.2 Error Handling
- Network error handling
- API error mapping
- User-friendly error messages
- Retry mechanisms

#### 9.3 Loading States
- Form submission loading
- Dry run loading
- Skeleton loaders for initial data

#### 9.4 Performance Optimization
- Memoize expensive calculations
- Debounce search inputs
- Optimize re-renders

## Implementation Checklist

### Phase 1: Foundation ✓
- [ ] Create type definitions
- [ ] Extend API client
- [ ] Set up file structure

### Phase 2: API Integration ✓
- [ ] Create TanStack Query hooks
- [ ] Test API integration
- [ ] Handle error responses

### Phase 3: Utilities ✓
- [ ] Implement validation logic
- [ ] Create CLI command generator
- [ ] Build offset request generator
- [ ] Add unit tests for utilities

### Phase 4: Components ✓
- [ ] Build TopicPartitionSelector
- [ ] Build OffsetValueSelector
- [ ] Build DateTimeInput
- [ ] Build CliCommandDisplay
- [ ] Build ResetOffsetForm
- [ ] Build DryRunResults
- [ ] Build ResetOffsetModal

### Phase 5: Integration ✓
- [ ] Update GroupsTable
- [ ] Update GroupDetailPage
- [ ] Add routing (if needed)
- [ ] Test navigation flow

### Phase 6: Internationalization ✓
- [ ] Add all translation keys
- [ ] Test with different locales
- [ ] Verify text rendering

### Phase 7: Testing ✓
- [ ] Write utility tests
- [ ] Write component tests
- [ ] Write integration tests
- [ ] Test error scenarios
- [ ] Test edge cases

### Phase 8: Documentation ✓
- [ ] Update user documentation
- [ ] Create developer documentation
- [ ] Add inline code comments
- [ ] Create usage examples

### Phase 9: Polish ✓
- [ ] Accessibility audit
- [ ] Error handling review
- [ ] Loading states
- [ ] Performance optimization
- [ ] Final QA testing

## Migration Strategy

### Approach
**Big Bang Migration** - Implement complete feature in new UI, then deprecate old UI version.

### Rollout Plan
1. **Development** (Days 1-10): Build and test feature
2. **Internal Testing** (Days 11-12): QA and bug fixes
3. **Beta Release**: Deploy to staging environment
4. **Production Release**: Deploy after successful beta testing

### Rollback Plan
If critical issues are discovered:
1. Feature flag to disable reset offset in new UI
2. Redirect users to old UI for this feature
3. Fix issues and re-deploy

## Risk Assessment

### Technical Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| API compatibility issues | High | Thorough API testing, maintain backward compatibility |
| Complex validation logic | Medium | Comprehensive unit tests, code review |
| Performance with large offset lists | Medium | Pagination, virtualization if needed |
| Browser compatibility | Low | Use PatternFly components (well-tested) |

### User Experience Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Confusing UI for complex scenarios | Medium | User testing, clear help text |
| Accidental offset resets | High | Dry run feature, confirmation dialogs |
| Error messages not clear | Medium | User-friendly error mapping |

## Success Criteria

### Functional Requirements
- ✅ All offset types supported (custom, earliest, latest, datetime, delete)
- ✅ Topic and partition selection works correctly
- ✅ Dry run preview shows accurate results
- ✅ CLI command generation is accurate
- ✅ Error handling is comprehensive
- ✅ Form validation prevents invalid submissions

### Non-Functional Requirements
- ✅ Form loads in < 1 second
- ✅ Dry run completes in < 3 seconds
- ✅ Offset reset completes in < 5 seconds
- ✅ WCAG 2.1 AA accessibility compliance
- ✅ Works in Chrome, Firefox, Safari, Edge
- ✅ Mobile responsive (if applicable)

### Quality Metrics
- ✅ 80%+ code coverage
- ✅ Zero critical bugs
- ✅ < 5 minor bugs
- ✅ User satisfaction > 4/5

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Foundation | 2 days | None |
| Phase 2: API Integration | 1 day | Phase 1 |
| Phase 3: Utilities | 1 day | Phase 1 |
| Phase 4: Components | 3 days | Phases 1-3 |
| Phase 5: Integration | 1 day | Phase 4 |
| Phase 6: i18n | 1 day | Phase 4 |
| Phase 7: Testing | 2 days | Phases 4-6 |
| Phase 8: Documentation | 1 day | Phase 7 |
| Phase 9: Polish | 1 day | Phase 7 |

**Total: 10-12 days**

## Resources

### Team
- 1 Frontend Developer (full-time)
- 1 QA Engineer (part-time, days 11-12)
- 1 Tech Writer (part-time, day 10)

### Tools
- React 18
- TypeScript
- TanStack Query
- PatternFly React
- Vitest (testing)
- React Testing Library

## References

### Old Implementation
- [`ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/`](../../../../../../ui/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/reset-offset/)
- [`ui/api/groups/actions.ts`](../../../../../../ui/api/groups/actions.ts)

### API Documentation
- Endpoint: `PATCH /api/kafkas/{kafkaId}/groups/{groupId}`
- [API Schema](../../../../../../ui/api/groups/schema.ts)

### Design Resources
- [PatternFly React Components](https://www.patternfly.org/components/all-components)
- [User Documentation](../../../../../../docs/sources/modules/proc-resetting-consumer-offsets.adoc)

## Appendix

### A. API Request/Response Examples

**Dry Run Request:**
```json
{
  "meta": {
    "dryRun": true
  },
  "data": {
    "type": "groups",
    "id": "my-consumer-group",
    "attributes": {
      "offsets": [
        {
          "topicId": "topic-123",
          "partition": 0,
          "offset": "earliest"
        }
      ]
    }
  }
}
```

**Dry Run Response:**
```json
{
  "data": {
    "id": "my-consumer-group",
    "type": "groups",
    "attributes": {
      "offsets": [
        {
          "topicId": "topic-123",
          "topicName": "my-topic",
          "partition": 0,
          "offset": 0
        }
      ]
    }
  }
}
```

### B. Validation Rules

1. **Custom Offset**
   - Requires specific topic AND specific partition
   - Must be non-negative integer
   - Must be within valid range (checked by API)

2. **Specific DateTime**
   - ISO 8601: Must match format `yyyy-MM-dd'T'HH:mm:ss.SSSZ`
   - Epoch: Must be positive integer (milliseconds)

3. **Delete Offsets**
   - Requires specific topic
   - Can apply to all partitions or specific partition

4. **Earliest/Latest**
   - Can apply to all topics or specific topic
   - Can apply to all partitions or specific partition

### C. Error Code Mapping

| API Error | User Message |
|-----------|--------------|
| Offset out of range | "Custom offset must be between {min} and {max}" |
| Partition not found | "Partition {partition} does not exist for topic {topic}" |
| Topic not found | "Topic {topic} does not exist" |
| Invalid datetime | "Please provide a valid UTC ISO timestamp" |
| Group not empty | "Consumer group must be in EMPTY state to reset offsets" |

---

**Document Version:** 1.0  
**Last Updated:** 2026-04-08  
**Author:** Migration Team  
**Status:** Ready for Implementation
