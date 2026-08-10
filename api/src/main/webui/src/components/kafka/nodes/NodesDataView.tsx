import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { ThProps } from '@patternfly/react-table';
import { UseQueryResult } from '@tanstack/react-query';
import {
  Flex,
  FlexItem,
  Label,
  Tooltip,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { Node, ListResponse, BrokerStatus, ControllerStatus, NodeListMeta } from '@/api/types';
import { ResourceListParams } from '@/api/hooks/useResourceList';
import {
  ResourceListDataView,
  ResourceListDataViewColumnMapper,
  ResourceListDataViewRowMapper,
  ResourceListDataViewRowResult,
} from '@/components/common/ResourceListDataView';
import {
  useRoleLabels,
  useBrokerStatusLabels,
  useControllerStatusLabels,
} from './NodeStatusLabel';
import { formatNumber } from '@/utils/format';

const columnNames = ['id', 'roles', 'status', 'replicas', 'rack', 'nodePool'] as const;

interface NodesDataViewProps {
  kafkaId: string;
  nodeResult: UseQueryResult<ListResponse<Node, NodeListMeta>, Error>;
  onDataViewChange: (params: ResourceListParams) => void;
}

export function NodesDataView({
  kafkaId,
  nodeResult,
  onDataViewChange,
}: NodesDataViewProps) {
  const { t } = useTranslation();
  const roleLabels = useRoleLabels();
  const brokerStatusLabels = useBrokerStatusLabels();
  const controllerStatusLabels = useControllerStatusLabels();

  const nodePoolFilterOptions = useMemo(() => {
    const nodePools = nodeResult.data?.meta?.summary?.nodePools;
    if (!nodePools) return [];
    return Object.entries(nodePools).map(([name, meta]) => ({
      value: name,
      label: (
        <>
          <Flex>
            <FlexItem>{name}</FlexItem>
            <FlexItem
              align={{ default: 'alignRight' }}
              style={{ color: 'var(--pf-t--global--text--color--subtle)' }}
            >
              {meta.count}
            </FlexItem>
          </Flex>
          <div style={{ fontSize: 'var(--pf-t--global--font--size--sm)', color: 'var(--pf-t--global--text--color--subtle)' }}>
            {t('nodes.filter.nodePoolRoles', { roles: meta.roles.join(', ') })}
          </div>
        </>
      ),
    }));
  }, [nodeResult.data?.meta, t]);

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
        cell: t('nodes.nodeId'),
        props: {
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
      { cell: t('nodes.roles') },
      { cell: t('nodes.status') },
      { cell: t('nodes.kafkaVersion') },
      {
        cell: (
          <>
            {t('nodes.replicas')}{' '}
            <Tooltip content={t('nodes.replicasTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: { modifier: 'fitContent', style: { textAlign: 'right' } },
      },
      {
        cell: (
          <>
            {t('nodes.leaders')}{' '}
            <Tooltip content={t('nodes.leadersTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
        props: { modifier: 'fitContent', style: { textAlign: 'right' } },
      },
      {
        cell: (
          <>
            {t('nodes.rack')}{' '}
            <Tooltip content={t('nodes.rackTooltip')}>
              <HelpIcon />
            </Tooltip>
          </>
        ),
      },
      { cell: t('nodes.nodePool') },
    ],
    [t, handleSort],
  );

  const colProvider = useMemo(() => ({
    dependencies: [t, handleSort],
    callback: colMapper,
  }), [colMapper, t, handleSort]);

  const rowMapper: ResourceListDataViewRowMapper<Node> = useCallback(
    (node): ResourceListDataViewRowResult => {
      return {
        row: [
          {
            cell: (
              <>
                {node.meta?.privileges?.includes('GET') === true ? (
                  <Link to={`/kafka/${kafkaId}/nodes/${node.id}/configuration`}>
                    {node.id}
                  </Link>
                ) : (
                  node.id
                )}
                {node.attributes.metadataState?.status === 'leader' && (
                  <Label isCompact color="green" className="pf-v6-u-ml-sm">
                    {t('nodes.leadController')}
                  </Label>
                )}
              </>
            ),
            props: { dataLabel: t('nodes.nodeId'), modifier: 'nowrap' },
          },
          {
            cell: (
              <>{node.attributes.roles?.map((role) => (
                <div key={role}>{roleLabels[role].label}</div>
              ))}</>
            ),
            props: { dataLabel: t('nodes.roles'), modifier: 'nowrap' },
          },
          {
            cell: (
              <>
                <div className="pf-v6-u-active-color-100">
                  {node.attributes.broker && brokerStatusLabels[node.attributes.broker.status]}
                </div>
                <div>
                  {node.attributes.controller && controllerStatusLabels[node.attributes.controller.status]}
                </div>
              </>
            ),
            props: { dataLabel: t('nodes.status'), modifier: 'nowrap' },
          },
          {
            cell: node.attributes.kafkaVersion,
            props: { dataLabel: t('nodes.kafkaVersion'), modifier: 'nowrap' },
          },
          {
            cell: typeof node.attributes.broker?.leaderCount === 'number' &&
              typeof node.attributes.broker?.replicaCount === 'number'
              ? formatNumber(node.attributes.broker.leaderCount + node.attributes.broker.replicaCount)
              : '-',
            props: { dataLabel: t('nodes.replicas'), modifier: 'fitContent', style: { textAlign: 'right' } },
          },
          {
            cell: typeof node.attributes.broker?.leaderCount === 'number'
              ? formatNumber(node.attributes.broker.leaderCount)
              : '-',
            props: { dataLabel: t('nodes.leaders'), modifier: 'fitContent', style: { textAlign: 'right' } },
          },
          {
            cell: node.attributes.rack || 'n/a',
            props: { dataLabel: t('nodes.rack'), modifier: 'nowrap' },
          },
          {
            cell: node.attributes.nodePool || 'n/a',
            props: { dataLabel: t('nodes.nodePool'), modifier: 'nowrap' },
          },
        ],
      };
    },
    [kafkaId, t, roleLabels, brokerStatusLabels, controllerStatusLabels],
  );

  const rowProvider = useMemo(() => ({
    dependencies: [kafkaId, t, roleLabels, brokerStatusLabels, controllerStatusLabels],
    callback: rowMapper,
  }), [rowMapper, kafkaId, t, roleLabels, brokerStatusLabels, controllerStatusLabels]);


  return (
    <ResourceListDataView
      resourceResult={nodeResult}
      onDataViewChange={onDataViewChange}
      ariaLabel={t('nodes.title')}
      ouiaIdPrefix="nodes"
      dataFilters={{
        nodePool: {
          type: 'checkbox',
          title: t('nodes.filter.nodePool'),
          placeholder: t('nodes.filter.nodePoolPlaceholder'),
          options: nodePoolFilterOptions,
        },
        roles: {
          type: 'checkbox',
          title: t('nodes.filter.role'),
          placeholder: t('nodes.filter.rolePlaceholder'),
          options: (
            ['broker', 'controller'] as const
          ).map((role) => ({
            value: role,
            label: roleLabels[role].label,
          })),
        },
        'broker.status': {
          type: 'checkbox',
          title: t('nodes.filter.brokerStatus'),
          placeholder: t('nodes.filter.statusPlaceholder'),
          options: (Object.keys(brokerStatusLabels) as BrokerStatus[]).map((status) => ({
            value: status,
            label: brokerStatusLabels[status],
          })),
        },
        'controller.status': {
          type: 'checkbox',
          title: t('nodes.filter.controllerStatus'),
          placeholder: t('nodes.filter.statusPlaceholder'),
          options: (Object.keys(controllerStatusLabels) as ControllerStatus[]).map((status) => ({
            value: status,
            label: controllerStatusLabels[status],
          })),
        },
      }}
      columnProvider={colProvider}
      rowProvider={rowProvider}
    />
  );
}
