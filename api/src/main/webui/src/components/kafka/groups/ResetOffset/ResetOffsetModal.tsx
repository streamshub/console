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
  ModalHeader,
  ModalBody,
  ModalFooter,
} from '@patternfly/react-core';
import { Group, OffsetResetResult } from '@/api/types';
import { OffsetValue, ResetOffsetFormState } from './types';
import { TopicPartitionSelector } from './TopicPartitionSelector';
import { OffsetValueSelector } from './OffsetValueSelector';
import { DryRunResults } from './DryRunResults';
import { useResetGroupOffsets } from '@/api/hooks/useGroups';
import {
  validateResetOffsetForm,
  generateOffsetRequests,
  isFormValid,
} from '@/utils/offsetValidation';
import { generateCliCommand } from '@/utils/cliCommandGenerator';
import { ApiError } from '@/api/client';
import { WrenchIcon } from '@patternfly/react-icons';

interface ResetOffsetModalProps {
  isOpen: boolean;
  group: Group;
  kafkaId: string;
  onClose: () => void;
  onSuccess?: (message?: string) => void;
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
  const [formState, setFormState] = useState<ResetOffsetFormState>({});

  // Dry run state
  const [showDryRun, setShowDryRun] = useState(false);
  const [dryRunResults, setDryRunResults] = useState<OffsetResetResult[]>([]);
  const [responseError, setResponseError] = useState<string>();

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

  const resetModalState = () => {
    setFormState({});
    setShowDryRun(false);
    setDryRunResults([]);
    setResponseError(undefined);
    resetMutation.reset();
  };

  // Generate CLI command
  const cliCommand = useMemo(() => {
    const selectedTopic = topics.find((t) => t.id === formState.selectedTopicId);
    return generateCliCommand(
      group.attributes.groupId,
      formState,
      selectedTopic?.name
    );
  }, [group.attributes.groupId, formState, topics]);

  const getResponseErrorMessage = (
    responseErrors?: Array<{ title: string; detail?: string }>
  ) => responseErrors?.map((error) => error.detail || error.title).join(' ') || 'Unknown error';

  // Handlers
  const handleTopicChange = (topicId?: string) => {
    if (!topicId) {
      setFormState((prev) => ({
        ...prev,
        selectedTopicId: undefined,
        selectedPartition: undefined,
      }));
      return;
    }

    setFormState((prev) => ({
      ...prev,
      selectedTopicId: topicId,
      selectedPartition: undefined,
    }));
  };

  const handlePartitionChange = (partition?: number) => {
    setFormState((prev) => ({
      ...prev,
      selectedPartition: partition,
    }));
  };

  const handleOffsetValueChange = (value?: OffsetValue) => {
    setFormState((prev) => ({
      ...prev,
      offsetValue: value,
      customOffset: undefined,
      dateTime: undefined,
      dateTimeDisplayMode: value === 'dateTimeIso' ? 'utc' : undefined,
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

  const handleDateTimeDisplayModeChange = (mode: 'utc' | 'local') => {
    setFormState((prev) => ({
      ...prev,
      dateTimeDisplayMode: mode,
    }));
  };

  const handleDryRun = async () => {
    const requests = generateOffsetRequests(formState, allPartitions);
    setResponseError(undefined);

    try {
      const response = await resetMutation.mutateAsync({
        offsets: requests,
        dryRun: true,
      });

      if (response.errors?.length) {
        setResponseError(getResponseErrorMessage(response.errors));
        setShowDryRun(false);
        return;
      }

      if (response.data) {
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
    setResponseError(undefined);
    setShowDryRun(false);

    try {
      const response = await resetMutation.mutateAsync({
        offsets: requests,
        dryRun: false,
      });

      if (response.errors?.length) {
        setResponseError(getResponseErrorMessage(response.errors));
        return;
      }

      resetModalState();
      onClose();
      onSuccess?.(t('groups.resetOffset.success', { groupId: group.attributes.groupId }));
    } catch (error) {
      console.error('Reset failed:', error);
    }
  };

  return (
    <>
      <Modal
        variant={ModalVariant.medium}
        isOpen={isOpen}
        onClose={() => {
          resetModalState();
          onClose();
        }}
      >
        <ModalHeader 
          title={t('groups.resetOffset.title', { groupId: group.attributes.groupId })}
          titleIconVariant={WrenchIcon}
          />
        <ModalBody>
          {(responseError || resetMutation.isError) && (
            <Alert
              variant={AlertVariant.danger}
              isInline
              title={t('groups.resetOffset.errors.generalError')}
              style={{ marginBottom: '1rem' }}
            >
              {responseError ??
                getResponseErrorMessage((resetMutation.error as ApiError)?.errors) ??
                resetMutation.error?.message ??
                'Unknown error'}
            </Alert>
          )}

          <Form isHorizontal>
            <FormSection title={t('groups.resetOffset.targetSelectionTitle')}>
              <TopicPartitionSelector
                topics={topicsList}
                partitions={selectedTopicPartitions}
                selectedTopicId={formState.selectedTopicId}
                selectedPartition={formState.selectedPartition}
                onTopicChange={handleTopicChange}
                onPartitionChange={handlePartitionChange}
              />
            </FormSection>

            <FormSection title={t('groups.resetOffset.offsetDetails')}>
              <OffsetValueSelector
                offsetValue={formState.offsetValue}
                customOffset={formState.customOffset}
                dateTime={formState.dateTime}
                dateTimeDisplayMode={formState.dateTimeDisplayMode}
                topicSelection={formState.selectedTopicId ? 'selectedTopic' : 'allTopics'}
                partitionSelection={formState.selectedPartition !== undefined ? 'selectedPartition' : 'allPartitions'}
                onOffsetValueChange={handleOffsetValueChange}
                onCustomOffsetChange={handleCustomOffsetChange}
                onDateTimeChange={handleDateTimeChange}
                onDateTimeDisplayModeChange={handleDateTimeDisplayModeChange}
                errors={{
                  customOffset: errors.CustomOffsetError,
                  dateTime: errors.SpecificDateTimeNotValidError,
                }}
              />
            </FormSection>

          </Form>
        </ModalBody>
        <ModalFooter>
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
          <Button
            variant="link"
            onClick={() => {
              resetModalState();
              onClose();
            }}
          >
            {t('common.cancel')}
          </Button>
        </ModalFooter>
      </Modal>

      <DryRunResults
        isOpen={showDryRun}
        results={dryRunResults}
        currentOffsets={group.attributes.offsets}
        command={cliCommand}
        onClose={() => setShowDryRun(false)}
      />
    </>
  );
}