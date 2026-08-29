import React, { useCallback, useMemo } from 'react';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ThProps, ActionsColumn } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import { Group, ListResponse, GroupType, GroupState } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
} from '@/components/common/ResourceListDataView';
import { Label, LabelGroup, Tooltip } from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { hasPrivilege } from '@/utils/privileges';
import { StatusLabel } from '@/components/StatusLabel';
import { GROUP_STATE_CONFIG } from '@/components/StatusLabel/configs';

const columnNames = ['groupId', 'type', 'protocol', 'state'] as const;

interface GroupsDataViewProps {
  groupsResult: UseQueryResult<ListResponse<Group>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
  onResetOffset: (group: Group) => void;
}

const GROUP_TYPES: GroupType[] = ['Classic', 'Consumer', 'Share', 'Streams'];

const GROUP_STATES: GroupState[] = [
  'STABLE',
  'EMPTY',
  'DEAD',
  'PREPARING_REBALANCE',
  'COMPLETING_REBALANCE',
  'ASSIGNING',
  'RECONCILING',
];

export function GroupsDataView({
  groupsResult,
  onDataViewChange,
  onResetOffset,
}: GroupsDataViewProps) {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();

  const handleDataViewChange = useCallback((params: ResourceListParams) => {
    const filters: Record<string, string | string[]> = {};

    Object.entries(params.filters ?? {}).forEach(([key, value]) => {
      if (typeof value === 'string' && value.length > 0) {
        filters[key] = value;
      } else if (Array.isArray(value) && value.length > 0) {
        filters[key] = value;
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

  const formatNumber = useCallback((value: number | undefined | null): string => {
    if (value === undefined || value === null || isNaN(value)) {
      return t('common.notAvailable');
    }
    return value.toLocaleString();
  }, [t]);

  const calculateLag = useCallback((group: Group): number | null => {
    if (!group.attributes.offsets) {
      return null;
    }
    
    const totalLag = group.attributes.offsets.reduce((acc, offset) => {
      const lag = offset.lag ?? NaN;
      return (acc ?? 0) + (isNaN(lag) ? 0 : lag);
    }, 0 as number | null);
    
    return totalLag;
  }, []);

  const getTopics = useCallback((group: Group): Record<string, string | undefined> => {
    const allTopics: Record<string, string | undefined> = {};
    
    group.attributes.members
      ?.flatMap((m) => m.assignments ?? [])
      .forEach((a) => (allTopics[a.topicName] = a.topicId));
    
    group.attributes.offsets?.forEach(
      (a) => (allTopics[a.topicName] = a.topicId)
    );
    
    return allTopics;
  }, []);

  const colMapper: ResourceListDataViewColumnMapper = useCallback(
    (sortBy, direction, onSort) => [
      {
        cell: t('groups.groupId'),
        props: {
          width: 30 as const,
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
        cell: (
          <>
            {t('groups.type')}{' '}
            <Tooltip content={t('groups.typeTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: {
          width: 15 as const,
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 1,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: (
          <>
            {t('groups.protocol')}{' '}
            <Tooltip content={t('groups.protocolTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: {
          width: 15 as const,
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 2,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: (
          <>
            {t('groups.state')}{' '}
            <Tooltip content={t('groups.stateTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: {
          width: 15 as const,
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 3,
            onSort: (event, columnIndex, sortDirection) =>
              handleSort(onSort, event, columnIndex, sortDirection),
          } as ThProps['sort'],
        },
      },
      {
        cell: (
          <>
            {t('groups.overallLag')}{' '}
            <Tooltip content={t('groups.overallLagTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: {
          width: 10 as const,
          modifier: 'fitContent' as const,
          style: { textAlign: 'right' },
        },
      },
      {
        cell: (
          <>
            {t('groups.members')}{' '}
            <Tooltip content={t('groups.membersTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: {
          width: 10 as const,
          modifier: 'fitContent' as const,
          style: { textAlign: 'right' },
        },
      },
      {
        cell: t('groups.topics'),
        props: {
          width: 25 as const,
        },
      },
      {
        cell: '',
        props: {},
      },
    ],
    [t, handleSort]
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<Group> = useCallback(
    (group) => {
      const state = group.attributes.state;
      const topics = getTopics(group);
      const lag = calculateLag(group);
      const canResetOffset =
        hasPrivilege('UPDATE', group) &&
        state === 'EMPTY' &&
        group.attributes.protocol === 'consumer';

      return {
        id: group.id,
        row: [
          {
            cell: group.meta?.describeAvailable ? (
              <Link to={`/kafka/${kafkaId}/groups/${group.id}`}>
                {group.attributes.groupId}
              </Link>
            ) : (
              group.attributes.groupId
            ),
            props: {
              dataLabel: t('groups.groupId'),
            },
          },
          {
            cell: group.attributes.type || '-',
            props: {
              dataLabel: t('groups.type'),
            },
          },
          {
            cell: group.attributes.protocol || '-',
            props: {
              dataLabel: t('groups.protocol'),
            },
          },
          {
            cell: <StatusLabel status={state} config={GROUP_STATE_CONFIG} />,
            props: {
              dataLabel: t('groups.state'),
            },
          },
          {
            cell: formatNumber(lag),
            props: {
              dataLabel: t('groups.overallLag'),
              modifier: 'fitContent' as const,
              style: { textAlign: 'right' },
            },
          },
          {
            cell: formatNumber(group.attributes.members?.length),
            props: {
              dataLabel: t('groups.members'),
              modifier: 'fitContent' as const,
              style: { textAlign: 'right' },
            },
          },
          {
            cell: (
              <LabelGroup>
                {Object.entries(topics).map(([topicName, topicId]) => (
                  <Label
                    key={topicName}
                    color="blue"
                    {...(topicId && {
                      render: ({ className, content }) => (
                        <Link to={`/kafka/${kafkaId}/topics/${topicId}`} className={className}>
                          {content}
                        </Link>
                      ),
                    })}
                  >
                    {topicName}
                  </Label>
                ))}
              </LabelGroup>
            ),
            props: {
              dataLabel: t('groups.topics'),
            },
          },
          {
            cell: (
              <ActionsColumn
                items={[
                  {
                    title: t('groups.resetOffsetAction'),
                    description:
                      group.attributes.protocol !== 'consumer'
                        ? t('groups.resetOffsetDisabledDescriptionNonConsumer', {
                            protocol: group.attributes.protocol,
                          })
                        : state === 'EMPTY'
                          ? undefined
                          : t('groups.resetOffsetDisabledDescription'),
                    onClick: () => onResetOffset(group),
                    isDisabled: !canResetOffset,
                  },
                ]}
              />
            ),
            props: {
              isActionCell: true,
            },
          },
        ],
      };
    },
    [kafkaId, t, formatNumber, calculateLag, getTopics, onResetOffset]
  );

  const rowProvider = useMemo(() => ({
    dependencies: [kafkaId, t, formatNumber, calculateLag, getTopics, onResetOffset],
    callback: rowMapper,
  }), [rowMapper, kafkaId, t, formatNumber, calculateLag, getTopics, onResetOffset]);

  return (
    <ResourceListDataView
      resourceResult={groupsResult}
      columnProvider={colProvider}
      rowProvider={rowProvider}
      onDataViewChange={handleDataViewChange}
      ariaLabel={t('groups.title')}
      ouiaIdPrefix="kafka-groups"
      dataFilters={{
        id: {
          type: 'text',
          title: t('groups.groupId'),
          placeholder: t('groups.filterByGroupId'),
        },
        type: {
          type: 'checkbox',
          title: t('groups.type'),
          placeholder: t('groups.type'),
          options: GROUP_TYPES.map((type) => ({
            value: type,
            label: type,
          })),
        },
        state: {
          type: 'checkbox',
          title: t('groups.state'),
          placeholder: t('groups.state'),
          options: GROUP_STATES.map((state) => ({
            value: state,
            label: <StatusLabel status={state} config={GROUP_STATE_CONFIG} />,
          })),
        },
      }}
    />
  );
}
