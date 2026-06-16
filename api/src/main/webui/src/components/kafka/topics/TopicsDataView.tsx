import { useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ThProps } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import { Topic, ListResponse } from '@/api/types';
import { ResourceListFilterValue, ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from '@/components/common/ResourceListDataView';
import { ManagedTopicLabel } from './ManagedTopicLabel';
import { StatusLabel } from '@/components/StatusLabel';
import { TOPIC_STATUS_CONFIG } from '@/components/StatusLabel/configs';
import type { TopicStatus } from '@/components/StatusLabel/configs';
import { formatBytes } from '@/utils/format';
import { HelpIcon } from '@patternfly/react-icons';
import { Tooltip } from '@patternfly/react-core';

const columnNames = ['name', 'status', 'numPartitions', 'groups', 'totalLeaderLogBytes'] as const;

interface TopicsDataViewProps {
  topicResult: UseQueryResult<ListResponse<Topic>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function TopicsDataView({
  topicResult,
  onDataViewChange,
}: TopicsDataViewProps) {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    const filters: Record<string, string | string[] | ResourceListFilterValue> = {};

    Object.entries(params.filters ?? {}).forEach(([key, value]) => {
      if (key === 'includeHidden') {
        if (value === 'true') {
          filters.visibility = {
            operator: 'in',
            value: 'internal,external',
          };
        }
        return;
      }

      if (Array.isArray(value)) {
        if (value.length > 0) {
          filters[key] = {
            operator: 'in',
            value: value.join(','),
          };
        }
        return;
      }

      if (typeof value === 'string' && value.length > 0) {
        filters[key] = key === 'id' ? {
          operator: 'eq',
          value,
        } : value ;
      }
    });

    onDataViewChange({
      ...params,
      filters,
    });
  }, [onDataViewChange]);

  const handleSort = useCallback((
    onSort: ((event: React.MouseEvent, sortBy: string, direction: 'asc' | 'desc') => void) | undefined,
    event: React.MouseEvent,
    columnIndex: number,
    direction: 'asc' | 'desc'
  ) => {
    onSort?.(event, columnNames[columnIndex], direction);
  }, []);

  const colMapper: ResourceListDataViewColumnMapper = useCallback(
    (sortBy, direction, onSort) => [
      {
        cell: t('topics.columnName'),
        props: {
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 0,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: 
          <>
            {t('topics.columnStatus')}
            {' '}
            <Tooltip content={t('topics.columnStatusTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>,
      },
      {
        cell: t('topics.columnPartitions'),
        props: {
          modifier: 'fitContent',
          style: { textAlign: 'right' },
        },
      },
      {
        cell: t('topics.columnGroups'),
        props: {
          modifier: 'fitContent',
          style: { textAlign: 'right' },
        },
      },
      {
        cell: t('topics.columnStorage'),
        props: {
          modifier: 'fitContent',
          style: { textAlign: 'right' },
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 4,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
    ],
    [t, handleSort]
  );

  const rowMapper: ResourceListDataViewRowMapper<Topic> = useCallback(
    (topic) => ({
      id: topic.id,
      row: [
        {
          cell: (
            <>
              <Link to={`/kafka/${kafkaId}/topics/${topic.id}`}>
                {topic.attributes.name}
              </Link>
              {topic.meta?.managed === true && <ManagedTopicLabel />}
            </>
          ),
          props: {
            dataLabel: t('topics.columnName'),
          },
        },
        {
          cell: topic.attributes.status ? (
            <StatusLabel status={topic.attributes.status} config={TOPIC_STATUS_CONFIG} />
          ) : '-',
          props: {
            dataLabel: t('topics.columnStatus'),
          },
        },
        {
          cell: topic.attributes.numPartitions !== null && topic.attributes.numPartitions !== undefined ? (
            <Link to={`/kafka/${kafkaId}/topics/${topic.id}/partitions`}>
              {topic.attributes.numPartitions}
            </Link>
          ) : '-',
          props: {
            dataLabel: t('topics.columnPartitions'),
            modifier: 'fitContent',
            style: { textAlign: 'right' },
          },
        },
        {
          cell: topic.relationships?.groups?.meta?.count !== undefined ? (
            <Link to={`/kafka/${kafkaId}/topics/${topic.id}/groups`}>
              {topic.relationships.groups.meta.count}
            </Link>
          ) : (
            topic.relationships?.groups?.meta?.count ?? '-'
          ),
          props: {
            dataLabel: t('topics.columnGroups'),
            modifier: 'fitContent',
            style: { textAlign: 'right' },
          },
        },
        {
          cell: formatBytes(topic.attributes.totalLeaderLogBytes),
          props: {
            dataLabel: t('topics.columnStorage'),
            modifier: 'fitContent',
            style: { textAlign: 'right' },
          },
        },
      ],
    }),
    [kafkaId, t]
  );

  return (
    <ResourceListDataView
      resourceResult={topicResult}
      onDataViewChange={handleDataViewChange}
      ariaLabel={t('topics.tableLabel')}
      ouiaIdPrefix="topics"
      dataFilters={{
        name: {
          type: 'text',
          title: t('topics.filter.name'),
          placeholder: t('topics.filter.namePlaceholder'),
        },
        status: {
          type: 'checkbox',
          title: t('topics.filter.status'),
          placeholder: t('topics.filter.selectStatus'),
          options: Object.keys(TOPIC_STATUS_CONFIG).map((status) => ({
            value: status,
            label: <StatusLabel status={status as TopicStatus} config={TOPIC_STATUS_CONFIG} />,
          })),
        },
        id: {
          type: 'text',
          title: t('topics.filter.topicId'),
          placeholder: t('topics.filter.idPlaceholder'),
        },
        includeHidden: {
          type: 'toggle',
          title: t('topics.showInternalTopics'),
          chipLabel: t('topics.showInternalTopics'),
          initialValue: false,
          label: (
            <>
              {t('topics.showInternalTopics')}{' '}
              <Tooltip content={t('topics.showInternalTopicsTooltip')}>
                <HelpIcon />
              </Tooltip>
            </>
          ),
        },
      }}
      columnProvider={{
        dependencies: [t, handleSort],
        callback: colMapper,
      }}
      rowProvider={{
        dependencies: [kafkaId, t],
        callback: rowMapper,
      }}
    />
  );
}