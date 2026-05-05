export type OffsetValue =
  | 'custom'
  | 'latest'
  | 'earliest'
  | 'dateTimeIso'
  | 'dateTimeEpoch'
  | 'delete';
export type TopicSelection = 'allTopics' | 'selectedTopic';
export type PartitionSelection = 'allPartitions' | 'selectedPartition';

export interface ResetOffsetFormState {
  selectedTopicId?: string;
  selectedPartition?: number;
  offsetValue?: OffsetValue;
  customOffset?: number;
  dateTime?: string;
  dateTimeDisplayMode?: 'utc' | 'local';
}

export type ResetOffsetErrorType =
  | 'CustomOffsetError'
  | 'PartitionError'
  | 'KafkaError'
  | 'SpecificDateTimeNotValidError'
  | 'GeneralError';

export interface ResetOffsetError {
  type: ResetOffsetErrorType;
  message: string;
}
