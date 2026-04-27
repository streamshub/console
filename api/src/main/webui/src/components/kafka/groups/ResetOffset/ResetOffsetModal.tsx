/**
 * Reset Offset Modal Component
 * Main orchestrator for the reset offset feature
 */

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
  Form,
  FormSection,
  Alert,
  AlertVariant,
} from '@patternfly/react-core';
import {
  ResetOffsetFormState,
  TopicSelection,
  PartitionSelection,
  OffsetValue,
  DateTimeFormat,
  Group,
  OffsetResetResult,
} from '@/api/types';
import { TopicPartitionSelector } from './TopicPartitionSelector';
import { OffsetValueSelector } from './OffsetValueSelector';
import { CliCommandDisplay } from './CliCommandDisplay';
import { DryRunResults } from './DryRunResults';
import { useResetGroupOffsets } from '@/api/hooks/useGroups';
import {
  validateResetOffsetForm,
  generateOffsetRequests,
  isFormValid,
} from '@/utils/offsetValidation';
import { generateCliCommand } from '@/utils/cliCommandGenerator';

interface ResetOffsetModalProps {
  isOpen: boolean;
  group: Group;
  kafkaId: string;
  onClose: () => void;
  onSuccess?: () => void;
}

export function ResetOffsetModal({
  isOpen,
  group,
  kafkaId,
  onClose,
  onSuccess,
}: ResetOffsetModalProps) {
  const { t } = useTranslation();
  const resetMutation = useResetGroupOffsets(kafkaId, group.id);

  // Form state
  const [formState, setFormState] = useState<ResetOffsetFormState>({
    topicSelection: 'allTopics',
    partitionSelection: 'allPartitions',
    offsetValue: 'latest',
    dateTimeFormat: 'ISO',
  });

  // Dry run state
  const [showDryRun, setShowDryRun] = useState(false);
  const [dryRunResults, setDryRunResults] = useState<OffsetResetResult[]>([]);

  // Derive topics from group offsets
  const topics = useMemo(() => {
    const topicMap = new Map<string, { id: string; name: string }>();
    if (group.attributes.offsets) {
      for (const offset of group.attributes.offsets) {
        if (offset.topicId && offset.topicName && !topicMap.has(offset.topicId)) {
          topicMap.set(offset.topicId, {
            id: offset.topicId,
            name: offset.topicName,
          });
        }
      }
    }
    return Array.from(topicMap.values());
  }, [group.attributes.offsets]);

  // Get all partitions from group offsets
  const allPartitions = useMemo(() => {
    const partitions: Array<{ topicId: string; partition: number }> = [];
    if (group.attributes.offsets) {
      for (const offset of group.attributes.offsets) {
        if (offset.topicId) {
          partitions.push({
            topicId: offset.topicId,
            partition: offset.partition,
          });
        }
      }
    }
    return partitions;
  }, [group.attributes.offsets]);

  // Get partitions for selected topic
  const selectedTopicPartitions = useMemo(() => {
    if (!formState.selectedTopicId) return [];
    return allPartitions
      .filter((p) => p.topicId === formState.selectedTopicId)
      .map((p) => p.partition)
      .sort((a, b) => a - b);
  }, [formState.selectedTopicId, allPartitions]);

  // Topics list is already in the correct format
  const topicsList = topics;

  // Validate form
  const errors = useMemo(() => {
    const validationErrors = validateResetOffsetForm(
      formState,
      selectedTopicPartitions
    );
    return validationErrors.reduce((acc, err) => {
      acc[err.type] = err.message;
      return acc;
    }, {} as Record<string, string>);
  }, [formState, selectedTopicPartitions]);

  const formIsValid = isFormValid(formState, selectedTopicPartitions);

  // Generate CLI command
  const cliCommand = useMemo(() => {
    const selectedTopic = topics.find((t) => t.id === formState.selectedTopicId);
    return generateCliCommand(
      group.attributes.groupId,
      formState,
      selectedTopic?.name
    );
  }, [group.attributes.groupId, formState, topics]);

  // Handlers
  const handleTopicSelectionChange = (selection: TopicSelection) => {
    setFormState((prev) => ({
      ...prev,
      topicSelection: selection,
      selectedTopicId: undefined,
      selectedTopicName: undefined,
      selectedPartition: undefined,
    }));
  };

  const handlePartitionSelectionChange = (selection: PartitionSelection) => {
    setFormState((prev) => ({
      ...prev,
      partitionSelection: selection,
      selectedPartition: undefined,
    }));
  };

  const handleTopicChange = (topicId: string) => {
    const topic = topics.find((t) => t.id === topicId);
    setFormState((prev) => ({
      ...prev,
      selectedTopicId: topicId,
      selectedTopicName: topic?.name,
      selectedPartition: undefined,
    }));
  };

  const handlePartitionChange = (partition: number) => {
    setFormState((prev) => ({
      ...prev,
      selectedPartition: partition,
    }));
  };

  const handleOffsetValueChange = (value: OffsetValue) => {
    setFormState((prev) => ({
      ...prev,
      offsetValue: value,
      customOffset: undefined,
      dateTime: undefined,
    }));
  };

  const handleCustomOffsetChange = (value: number) => {
    setFormState((prev) => ({
      ...prev,
      customOffset: value,
    }));
  };

  const handleDateTimeChange = (value: string) => {
    setFormState((prev) => ({
      ...prev,
      dateTime: value,
    }));
  };

  const handleDateTimeFormatChange = (format: DateTimeFormat) => {
    setFormState((prev) => ({
      ...prev,
      dateTimeFormat: format,
      dateTime: undefined,
    }));
  };

  const handleDryRun = async () => {
    const requests = generateOffsetRequests(formState, allPartitions);
    
    try {
      const response = await resetMutation.mutateAsync({
        offsets: requests,
        dryRun: true,
      });

      if (response.data) {
        // Extract dry run results from response
        const results = response.data.attributes.offsets || [];
        setDryRunResults(results);
        setShowDryRun(true);
      }
    } catch (error) {
      console.error('Dry run failed:', error);
    }
  };

  const handleApply = async () => {
    const requests = generateOffsetRequests(formState, allPartitions);
    
    try {
      await resetMutation.mutateAsync({
        offsets: requests,
        dryRun: false,
      });

      onSuccess?.();
      onClose();
    } catch (error) {
      console.error('Reset failed:', error);
    }
  };

  const handleReset = () => {
    setFormState({
      topicSelection: 'allTopics',
      partitionSelection: 'allPartitions',
      offsetValue: 'latest',
      dateTimeFormat: 'ISO',
    });
  };

  return (
    <>
      <Modal
        variant={ModalVariant.large}
        title={t('groups.resetOffset.title')}
        isOpen={isOpen}
        onClose={onClose}
      >
        <div style={{ padding: '1.5rem' }}>
          {resetMutation.isError && (
            <Alert
              variant={AlertVariant.danger}
              isInline
              title={t('groups.resetOffset.errors.generalError')}
              style={{ marginBottom: '1rem' }}
            >
              {resetMutation.error?.message || 'Unknown error'}
            </Alert>
          )}

          <Form>
            <FormSection title={t('groups.resetOffset.targetWithGroupId', { groupId: group.attributes.groupId })}>
              <TopicPartitionSelector
                topics={topicsList}
                partitions={selectedTopicPartitions}
                topicSelection={formState.topicSelection}
                partitionSelection={formState.partitionSelection}
                selectedTopicId={formState.selectedTopicId}
                selectedPartition={formState.selectedPartition}
                onTopicSelectionChange={handleTopicSelectionChange}
                onPartitionSelectionChange={handlePartitionSelectionChange}
                onTopicChange={handleTopicChange}
                onPartitionChange={handlePartitionChange}
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
                onOffsetValueChange={handleOffsetValueChange}
                onCustomOffsetChange={handleCustomOffsetChange}
                onDateTimeChange={handleDateTimeChange}
                onDateTimeFormatChange={handleDateTimeFormatChange}
                errors={{
                  customOffset: errors.CustomOffsetError,
                  dateTime: errors.SpecificDateTimeNotValidError,
                }}
              />
            </FormSection>

            <CliCommandDisplay command={cliCommand} />
          </Form>
        </div>

        <div style={{ padding: '1.5rem', paddingTop: '0', display: 'flex', gap: '0.5rem' }}>
          <Button
            variant="primary"
            onClick={handleApply}
            isDisabled={!formIsValid || resetMutation.isPending}
            isLoading={resetMutation.isPending}
          >
            {t('groups.resetOffset.reset')}
          </Button>
          <Button
            variant="secondary"
            onClick={handleDryRun}
            isDisabled={!formIsValid || resetMutation.isPending}
          >
            {t('groups.resetOffset.dryRun')}
          </Button>
          <Button variant="link" onClick={handleReset}>
            {t('common.clear')}
          </Button>
          <Button variant="link" onClick={onClose}>
            {t('common.cancel')}
          </Button>
        </div>
      </Modal>

      <DryRunResults
        isOpen={showDryRun}
        results={dryRunResults}
        onClose={() => setShowDryRun(false)}
        onApply={handleApply}
        isApplying={resetMutation.isPending}
      />
    </>
  );
}