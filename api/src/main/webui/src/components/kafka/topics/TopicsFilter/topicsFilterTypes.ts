/**
 * Topics Filter Types for Advanced Filtering
 */

export type TopicStatus = 'FullyReplicated' | 'UnderReplicated' | 'PartiallyOffline' | 'Offline' | 'Unknown';

export type TopicsFilterType = 'name' | 'id' | 'status';

export interface TopicsFilterState {
  name?: string;
  id?: string;
  status?: TopicStatus[];
}

export interface TopicsFilterChip {
  type: TopicsFilterType;
  value: string;
  label: string;
}