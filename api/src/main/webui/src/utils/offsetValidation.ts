/**
 * Validation utilities for reset offset functionality
 */

import {
  ResetOffsetFormState,
  ResetOffsetError,
  OffsetResetRequest,
} from '../api/types';

/**
 * Validate the reset offset form state
 */
export function validateResetOffsetForm(
  state: ResetOffsetFormState,
  availablePartitions: number[]
): ResetOffsetError[] {
  const errors: ResetOffsetError[] = [];

  // Topic validation
  if (state.topicSelection === 'selectedTopic' && !state.selectedTopicId) {
    errors.push({
      type: 'GeneralError',
      message: 'Please select a topic',
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
      message: 'Please select a partition',
    });
  }

  // Validate partition exists
  if (
    state.topicSelection === 'selectedTopic' &&
    state.partitionSelection === 'selectedPartition' &&
    state.selectedPartition !== undefined &&
    !availablePartitions.includes(state.selectedPartition)
  ) {
    errors.push({
      type: 'PartitionError',
      message: `Partition ${state.selectedPartition} does not exist for this topic`,
    });
  }

  // Custom offset validation
  if (state.offsetValue === 'custom') {
    if (
      state.topicSelection !== 'selectedTopic' ||
      state.partitionSelection !== 'selectedPartition'
    ) {
      errors.push({
        type: 'CustomOffsetError',
        message: 'Custom offset requires specific topic and partition',
      });
    }
    if (state.customOffset === undefined || state.customOffset < 0) {
      errors.push({
        type: 'CustomOffsetError',
        message: 'Please enter a valid offset value (must be >= 0)',
      });
    }
  }

  // DateTime validation
  if (state.offsetValue === 'specificDateTime') {
    if (!state.dateTime) {
      errors.push({
        type: 'SpecificDateTimeNotValidError',
        message: 'Please enter a date and time',
      });
    } else if (state.dateTimeFormat === 'ISO') {
      // Validate ISO 8601 format
      if (!isValidISODateTime(state.dateTime)) {
        errors.push({
          type: 'SpecificDateTimeNotValidError',
          message: 'Invalid ISO 8601 format. Expected: yyyy-MM-ddTHH:mm:ss.SSSZ',
        });
      }
    } else if (state.dateTimeFormat === 'Epoch') {
      // Validate epoch timestamp
      const epoch = Number(state.dateTime);
      if (isNaN(epoch) || epoch < 0) {
        errors.push({
          type: 'SpecificDateTimeNotValidError',
          message: 'Invalid epoch timestamp. Must be a positive number',
        });
      }
    }
  }

  // Delete offset validation
  if (state.offsetValue === 'delete') {
    if (state.topicSelection !== 'selectedTopic') {
      errors.push({
        type: 'GeneralError',
        message: 'Delete offset requires a specific topic to be selected',
      });
    }
  }

  return errors;
}

/**
 * Validate ISO 8601 date time format
 */
export function isValidISODateTime(dateTime: string): boolean {
  // ISO 8601 format: yyyy-MM-ddTHH:mm:ss.SSSZ or yyyy-MM-ddTHH:mm:ss.SSS+HH:mm
  const isoRegex =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?([+-]\d{2}:\d{2}|Z)?$/;

  if (!isoRegex.test(dateTime)) {
    return false;
  }

  // Try to parse the date to ensure it's valid
  try {
    const date = new Date(dateTime);
    return !isNaN(date.getTime());
  } catch {
    return false;
  }
}

/**
 * Convert epoch timestamp to ISO 8601 format
 */
export function convertEpochToISO(epoch: string | number): string {
  const timestamp = typeof epoch === 'string' ? parseInt(epoch, 10) : epoch;
  const date = new Date(timestamp);
  return date.toISOString();
}

/**
 * Check if form is valid and ready to submit
 */
export function isFormValid(
  state: ResetOffsetFormState,
  availablePartitions: number[]
): boolean {
  const errors = validateResetOffsetForm(state, availablePartitions);
  return errors.length === 0;
}

/**
 * Generate offset reset requests from form state
 */
export function generateOffsetRequests(
  state: ResetOffsetFormState,
  allPartitions: Array<{ topicId: string; partition: number }>
): OffsetResetRequest[] {
  const requests: OffsetResetRequest[] = [];

  // Determine the offset value to use
  let offsetValue: string | number | null;

  switch (state.offsetValue) {
    case 'custom':
      offsetValue = state.customOffset ?? 0;
      break;
    case 'delete':
      offsetValue = null;
      break;
    case 'specificDateTime':
      if (state.dateTimeFormat === 'Epoch' && state.dateTime) {
        offsetValue = convertEpochToISO(state.dateTime);
      } else {
        offsetValue = state.dateTime ?? '';
      }
      break;
    case 'earliest':
    case 'latest':
    default:
      offsetValue = state.offsetValue;
      break;
  }

  // Generate requests based on topic/partition selection
  if (state.topicSelection === 'allTopics') {
    // Apply to all partitions of all topics
    for (const partition of allPartitions) {
      requests.push({
        topicId: partition.topicId,
        partition: partition.partition,
        offset: offsetValue,
      });
    }
  } else if (state.selectedTopicId) {
    // Apply to specific topic
    const topicPartitions = allPartitions.filter(
      (p) => p.topicId === state.selectedTopicId
    );

    if (state.partitionSelection === 'allPartitions') {
      // Apply to all partitions of the selected topic
      for (const partition of topicPartitions) {
        requests.push({
          topicId: partition.topicId,
          partition: partition.partition,
          offset: offsetValue,
        });
      }
    } else if (state.selectedPartition !== undefined) {
      // Apply to specific partition
      requests.push({
        topicId: state.selectedTopicId,
        partition: state.selectedPartition,
        offset: offsetValue,
      });
    }
  }

  // Remove duplicates (shouldn't happen, but just in case)
  return requests.filter(
    (value, index, self) =>
      index ===
      self.findIndex(
        (t) => t.topicId === value.topicId && t.partition === value.partition
      )
  );
}