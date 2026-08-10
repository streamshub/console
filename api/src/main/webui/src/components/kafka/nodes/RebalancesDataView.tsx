import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { DataViewTd } from '@patternfly/react-data-view';
import { ThProps, ActionsColumn } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  List,
  ListItem,
  Popover,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { Rebalance, ListResponse } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
  ResourceListDataViewRowResult,
} from '@/components/common/ResourceListDataView';
import { StatusLabel } from '@/components/StatusLabel';
import { createRebalanceStatusConfig } from '@/components/StatusLabel/configs';
import { hasPrivilege } from '@/utils/privileges';
import { formatDateTime } from '@/utils/dateTime';

const columnNames = ['name', 'status', 'lastUpdated', 'dataToMove', 'partitionsToMove', 'leadershipUpdates', 'estTimeToComplete'] as const;

function getLastUpdated(rebalance: Rebalance): string {
  const statusCondition = rebalance.attributes.conditions?.find(
    (c) => c.type === rebalance.attributes.status,
  );
  return statusCondition?.lastTransitionTime || rebalance.attributes.creationTimestamp || '';
}

interface RebalancesDataViewProps {
  kafkaId: string;
  rebalanceResult: UseQueryResult<ListResponse<Rebalance>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
  onApprove: (rebalance: Rebalance) => void;
  onStop: (rebalance: Rebalance) => void;
  onRefresh: (rebalance: Rebalance) => void;
  onViewDetails: (rebalance: Rebalance) => void;
}

export function RebalancesDataView({
  kafkaId,
  rebalanceResult,
  onDataViewChange,
  onApprove,
  onStop,
  onRefresh,
  onViewDetails,
}: RebalancesDataViewProps) {
  const { t } = useTranslation();
  const statusConfig = useMemo(() => createRebalanceStatusConfig(t), [t]);

  const handleSort = useCallback((
    onSort: ((event: React.MouseEvent, sortBy: string, direction: 'asc' | 'desc') => void) | undefined,
    event: React.MouseEvent,
    columnIndex: number,
    direction: 'asc' | 'desc',
  ) => {
    onSort?.(event, columnNames[columnIndex], direction);
  }, []);

  const colMapper: ResourceListDataViewColumnMapper = useCallback(
    (sortBy, direction, onSort) => [
      {
        cell: t('rebalancing.rebalanceName'),
        props: {
          width: 30,
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 0,
            onSort: (event, columnIndex, dir) => handleSort(onSort, event, columnIndex, dir),
          } as ThProps['sort'],
        },
      },
      {
        cell: t('rebalancing.status'),
        props: {
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 1,
            onSort: (event, columnIndex, dir) => handleSort(onSort, event, columnIndex, dir),
          } as ThProps['sort'],
        },
      },
      { cell: t('rebalancing.dataToMove'), props: { modifier: 'nowrap' } },
      { cell: t('rebalancing.partitionsToMove'), props: { modifier: 'nowrap' } },
      { cell: t('rebalancing.leadershipUpdates'), props: { modifier: 'nowrap' } },
      /* { cell: t('rebalancing.estTimeToComplete'), props: { modifier: 'nowrap' } }, */
      {
        cell: t('rebalancing.lastUpdated'),
        props: {
          sort: {
            sortBy: {
              index: sortBy ? columnNames.indexOf(sortBy as typeof columnNames[number]) : undefined,
              direction,
            },
            columnIndex: 2,
            onSort: (event, columnIndex, dir) => handleSort(onSort, event, columnIndex, dir),
          } as ThProps['sort'],
        },
      },
      { cell: '' }, // actions column
    ],
    [t, handleSort],
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<Rebalance> = useCallback(
    (rebalance): ResourceListDataViewRowResult => {
      const canUpdate = hasPrivilege('UPDATE', rebalance);
      const lastUpdated = getLastUpdated(rebalance);

      return {
        row: {
          id: rebalance.id,
          row: [
            {
              id: rebalance.id,
              cell: (
                <Button variant="link" isInline onClick={() => onViewDetails(rebalance)}>
                  {rebalance.attributes.name}
                </Button>
              ),
              props: { dataLabel: t('rebalancing.rebalanceName') },
            } as DataViewTd,
            {
              cell: (
                <StatusLabel
                  status={rebalance.attributes.status || 'New'}
                  config={statusConfig}
                />
              ),
              props: { dataLabel: t('rebalancing.status') },
            },
            {
              cell: rebalance.attributes.optimizationResult?.dataToMoveMB != null
                ? `${rebalance.attributes.optimizationResult.dataToMoveMB} MB`
                : '–',
              props: { dataLabel: t('rebalancing.dataToMove') },
            },
            {
              cell: rebalance.attributes.optimizationResult?.numReplicaMovements ?? '–',
              props: { dataLabel: t('rebalancing.partitionsToMove') },
            },
            {
              cell: rebalance.attributes.optimizationResult?.numLeaderMovements ?? '–',
              props: { dataLabel: t('rebalancing.leadershipUpdates') },
            },
            /* {
              cell: '–',
              props: { dataLabel: t('rebalancing.estTimeToComplete') },
            }, */
            {
              cell: formatDateTime({ value: lastUpdated }),
              props: { dataLabel: t('rebalancing.lastUpdated') },
            },
            {
              cell: (
                <ActionsColumn
                  isDisabled={!canUpdate}
                  items={[
                    {
                      title: t('rebalancing.approve'),
                      onClick: () => onApprove(rebalance),
                      isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('approve'),
                    },
                    {
                      title: t('rebalancing.refresh'),
                      onClick: () => onRefresh(rebalance),
                      isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('refresh'),
                    },
                    {
                      title: t('rebalancing.stop'),
                      onClick: () => onStop(rebalance),
                      isDisabled: !canUpdate || !rebalance.meta?.allowedActions?.includes('stop'),
                    },
                  ]}
                />
              ),
              props: { isActionCell: true },
            },
          ],
        },
        expandedRows: [{
          rowId: rebalance.id as unknown as number,
          columnId: 0,
          content: (
            <DescriptionList className="pf-v6-u-mt-md pf-v6-u-mb-lg">
              <Flex justifyContent={{ default: 'justifyContentSpaceEvenly' }}>
                <FlexItem style={{ width: '25%' }}>
                  <DescriptionListGroup>
                    <DescriptionListTerm>{t('rebalancing.autoApprovalEnabled')}</DescriptionListTerm>
                    <DescriptionListDescription>
                      {rebalance.meta?.autoApproval === true ? 'true' : 'false'}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </FlexItem>
                <FlexItem style={{ width: '50%', paddingRight: '5rem' }}>
                  <DescriptionListGroup>
                    <DescriptionListTerm>
                      {t('rebalancing.mode')}{' '}
                      <Popover
                        aria-label={t('rebalancing.mode')}
                        headerContent={<div>{t('rebalancing.rebalanceMode')}</div>}
                        bodyContent={
                          <div>
                            <List>
                              <ListItem>
                                <strong>{t('rebalancing.fullMode')}</strong>{' '}
                                {t('rebalancing.fullModeDescription')}
                              </ListItem>
                              <ListItem>
                                <strong>{t('rebalancing.addBrokersMode')}</strong>{' '}
                                {t('rebalancing.addBrokersModeDescription')}
                              </ListItem>
                              <ListItem>
                                <strong>{t('rebalancing.removeBrokersMode')}</strong>{' '}
                                {t('rebalancing.removeBrokersModeDescription')}
                              </ListItem>
                            </List>
                          </div>
                        }
                      >
                        <HelpIcon />
                      </Popover>
                    </DescriptionListTerm>
                    <DescriptionListDescription>
                      {rebalance.attributes.mode === 'full' ? (
                        t('rebalancing.fullMode')
                      ) : (
                        <>
                          {rebalance.attributes.mode === 'add-brokers'
                            ? t('rebalancing.addBrokersMode')
                            : t('rebalancing.removeBrokersMode')}{' '}
                          {rebalance.attributes.brokers?.length
                            ? rebalance.attributes.brokers.map((b, index) => (
                                <span key={b}>
                                  <Link to={`/kafka/${kafkaId}/nodes/${b}`}>
                                    {t('rebalancing.broker', { b })}
                                  </Link>
                                  {index < (rebalance.attributes.brokers?.length || 0) - 1 && ', '}
                                </span>
                              ))
                            : ''}
                        </>
                      )}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </FlexItem>
              </Flex>
            </DescriptionList>
          ),
        }],
      };
    },
    [kafkaId, t, statusConfig, onApprove, onStop, onRefresh, onViewDetails],
  );

  const rowProvider = useMemo(() => ({
    dependencies: [kafkaId, t, statusConfig, onApprove, onStop, onRefresh, onViewDetails],
    callback: rowMapper,
  }), [rowMapper, kafkaId, t, statusConfig, onApprove, onStop, onRefresh, onViewDetails]);

  return (
    <ResourceListDataView
      resourceResult={rebalanceResult}
      onDataViewChange={onDataViewChange}
      ariaLabel={t('rebalancing.title')}
      ouiaIdPrefix="rebalances"
      dataFilters={{
        name: {
          type: 'text',
          title: t('rebalancing.rebalanceName'),
          placeholder: t('common.filter.namePlaceholder'),
        },
        status: {
          type: 'checkbox',
          title: t('rebalancing.status'),
          placeholder: t('common.filter.statusPlaceholder'),
          options: (
            ['New', 'PendingProposal', 'ProposalReady', 'Stopped',
             'Rebalancing', 'NotReady', 'Ready', 'ReconciliationPaused'] as const
          ).map((s) => ({
            value: s,
            label: <StatusLabel status={s} config={statusConfig} />,
          })),
        },
        mode: {
          type: 'checkbox',
          title: t('rebalancing.mode'),
          placeholder: t('rebalancing.filter.modePlaceholder'),
          options: [
            { value: 'full',           label: t('rebalancing.fullMode') },
            { value: 'add-brokers',    label: t('rebalancing.addBrokersMode') },
            { value: 'remove-brokers', label: t('rebalancing.removeBrokersMode') },
          ],
        },
      }}
      columnProvider={colProvider}
      rowProvider={rowProvider}
    />
  );
}
