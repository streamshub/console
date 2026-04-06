/**
 * Groups Table Component
 * Displays a table of groups with filtering and sorting
 */

import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import {
  Label,
  LabelGroup,
  Icon,
  Tooltip,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ThProps,
} from '@patternfly/react-table';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InfoCircleIcon,
  OffIcon,
  PendingIcon,
  HistoryIcon,
  SyncAltIcon,
  InProgressIcon,
  HelpIcon,
} from '@patternfly/react-icons';
import { Group, GroupState } from '../api/types';

type SortableColumn = 'groupId' | 'type' | 'protocol' | 'state';

interface GroupsTableProps {
  groups: Group[];
  kafkaId: string;
  sortBy?: SortableColumn;
  sortDirection?: 'asc' | 'desc';
  onSort?: (column: SortableColumn) => void;
}

const StateLabel: Record<GroupState, { label: React.ReactNode }> = {
  STABLE: {
    label: (
      <>
        <Icon status="success">
          <CheckCircleIcon />
        </Icon>
        &nbsp;Stable
      </>
    ),
  },
  EMPTY: {
    label: (
      <>
        <Icon status="info">
          <InfoCircleIcon />
        </Icon>
        &nbsp;Empty
      </>
    ),
  },
  UNKNOWN: {
    label: (
      <>
        <Icon status="warning">
          <ExclamationTriangleIcon />
        </Icon>
        &nbsp;Unknown
      </>
    ),
  },
  PREPARING_REBALANCE: {
    label: (
      <>
        <Icon>
          <PendingIcon />
        </Icon>
        &nbsp;Preparing Rebalance
      </>
    ),
  },
  ASSIGNING: {
    label: (
      <>
        <Icon>
          <HistoryIcon />
        </Icon>
        &nbsp;Assigning
      </>
    ),
  },
  DEAD: {
    label: (
      <>
        <Icon>
          <OffIcon />
        </Icon>
        &nbsp;Dead
      </>
    ),
  },
  COMPLETING_REBALANCE: {
    label: (
      <>
        <Icon>
          <SyncAltIcon />
        </Icon>
        &nbsp;Completing Rebalance
      </>
    ),
  },
  RECONCILING: {
    label: (
      <>
        <Icon>
          <InProgressIcon />
        </Icon>
        &nbsp;Reconciling
      </>
    ),
  },
};

export function GroupsTable({ groups, kafkaId, sortBy, sortDirection, onSort }: GroupsTableProps) {
  const { t } = useTranslation();

  const getSortParams = (columnName: SortableColumn): ThProps['sort'] | undefined => {
    if (!onSort) return undefined;
    
    return {
      sortBy: {
        index: sortBy === columnName ? 0 : undefined,
        direction: sortDirection,
      },
      onSort: () => onSort(columnName),
      columnIndex: 0,
    };
  };

  const formatNumber = (value: number | undefined | null): string => {
    if (value === undefined || value === null || isNaN(value)) {
      return t('common.notAvailable');
    }
    return value.toLocaleString();
  };

  const calculateLag = (group: Group): number | null => {
    if (!group.attributes.offsets) {
      return null;
    }
    
    const totalLag = group.attributes.offsets.reduce((acc, offset) => {
      const lag = offset.lag ?? NaN;
      return (acc ?? 0) + (isNaN(lag) ? 0 : lag);
    }, 0 as number | null);
    
    return totalLag;
  };

  const getTopics = (group: Group): Record<string, string | undefined> => {
    const allTopics: Record<string, string | undefined> = {};
    
    group.attributes.members
      ?.flatMap((m) => m.assignments ?? [])
      .forEach((a) => (allTopics[a.topicName] = a.topicId));
    
    group.attributes.offsets?.forEach(
      (a) => (allTopics[a.topicName] = a.topicId)
    );
    
    return allTopics;
  };

  return (
    <Table aria-label={t('groups.title')} variant="compact">
      <Thead>
        <Tr>
          <Th width={30} sort={getSortParams('groupId')}>
            {t('groups.groupId')}
          </Th>
          <Th width={15} sort={getSortParams('type')}>
            {t('groups.type')}{' '}
            <Tooltip content={t('groups.typeTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={15} sort={getSortParams('protocol')}>
            {t('groups.protocol')}{' '}
            <Tooltip content={t('groups.protocolTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={15} sort={getSortParams('state')}>
            {t('groups.state')}{' '}
            <Tooltip content={t('groups.stateTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={10} modifier="fitContent" style={{ textAlign: 'right' }}>
            {t('groups.overallLag')}{' '}
            <Tooltip content={t('groups.overallLagTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={10} modifier="fitContent" style={{ textAlign: 'right' }}>
            {t('groups.members')}{' '}
            <Tooltip content={t('groups.membersTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th width={25}>{t('groups.topics')}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {groups.map((group) => {
          const state = group.attributes.state;
          const topics = getTopics(group);
          const lag = calculateLag(group);

          return (
            <Tr key={group.id}>
              <Td dataLabel={t('groups.groupId')}>
                {group.meta?.describeAvailable ? (
                  <Link to={`/kafka/${kafkaId}/groups/${group.id}`}>
                    {group.attributes.groupId}
                  </Link>
                ) : (
                  group.attributes.groupId
                )}
              </Td>
              <Td dataLabel={t('groups.type')}>
                {group.attributes.type || '-'}
              </Td>
              <Td dataLabel={t('groups.protocol')}>
                {group.attributes.protocol || '-'}
              </Td>
              <Td dataLabel={t('groups.state')}>
                {StateLabel[state]?.label}
              </Td>
              <Td dataLabel={t('groups.overallLag')} modifier="fitContent" style={{ textAlign: 'right' }}>
                {formatNumber(lag)}
              </Td>
              <Td dataLabel={t('groups.members')} modifier="fitContent" style={{ textAlign: 'right' }}>
                {formatNumber(group.attributes.members?.length)}
              </Td>
              <Td dataLabel={t('groups.topics')}>
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
              </Td>
            </Tr>
          );
        })}
      </Tbody>
    </Table>
  );
}