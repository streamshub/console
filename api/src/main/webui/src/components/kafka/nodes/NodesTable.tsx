/**
 * Nodes Table Component
 * Displays Kafka cluster nodes with sorting and expandable rows
 */

import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ExpandableRowContent,
  ThProps,
} from '@patternfly/react-table';
import {
  EmptyState,
  EmptyStateBody,
  Title,
  Flex,
  FlexItem,
  Content,
  ClipboardCopy,
  Tooltip,
  Button,
  Label,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { ChartDonutUtilization } from '@patternfly/react-charts/victory';
import type { Node } from '@/api/types';
import {
  useRoleLabels,
  useBrokerStatusLabels,
  useControllerStatusLabels,
} from './NodeStatusLabel';
import { formatNumber } from '@/utils/format';
import { Link } from 'react-router-dom';

interface NodesTableProps {
  kafkaId: string;
  nodes: Node[];
  isLoading?: boolean;
  hasActiveFilters?: boolean;
  onClearAllFilters?: () => void;
  onSort?: (column: string) => void;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

export function NodesTable({
  kafkaId,
  nodes,
  isLoading,
  hasActiveFilters = false,
  onClearAllFilters,
  onSort,
  sortBy,
  sortDirection = 'asc',
}: NodesTableProps) {
  const { t } = useTranslation();
  const roleLabels = useRoleLabels();
  const brokerStatusLabels = useBrokerStatusLabels();
  const controllerStatusLabels = useControllerStatusLabels();

  // Expandable row state
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  // Toggle row expansion
  const toggleRowExpansion = (nodeId: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  };

  // Get sort params for column
  const getSortParams = (columnKey: string): ThProps['sort'] | undefined => {
    if (!onSort) return undefined;
    
    return {
      sortBy: {
        index: sortBy === columnKey ? 0 : undefined,
        direction: sortDirection,
      },
      onSort: () => onSort(columnKey),
      columnIndex: 0,
    };
  };

  // Format bytes
  const formatBytes = (bytes?: number): string => {
    if (bytes === undefined || bytes === null) return 'N/A';
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  // Empty state
  if (nodes.length === 0 && !isLoading) {
    return (
      <EmptyState>
        <Title headingLevel="h2" size="lg">
          {hasActiveFilters ? t('topics.filter.clearAllFilters') : t('nodes.noNodes')}
        </Title>
        <EmptyStateBody>
          {hasActiveFilters
            ? 'No nodes match the current filter criteria.'
            : t('nodes.noNodesDescription')}
        </EmptyStateBody>
        {hasActiveFilters && onClearAllFilters && (
          <Button onClick={onClearAllFilters}>{t('topics.filter.clearAllFilters')}</Button>
        )}
      </EmptyState>
    );
  }

  return (
    <Table ouiaId={"nodes-listing"} aria-label={t('nodes.title')} variant="compact">
      <Thead>
        <Tr>
          <Th modifier="fitContent" />
          <Th sort={getSortParams('id')}>{t('nodes.nodeId')}</Th>
          <Th>{t('nodes.roles')}</Th>
          <Th>{t('nodes.status')}</Th>
          <Th sort={getSortParams('replicas')} modifier="fitContent" style={{ textAlign: 'right' }}>
            {t('nodes.replicas')}{' '}
            <Tooltip content={t('nodes.replicasTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th sort={getSortParams('rack')}>
            {t('nodes.rack')}{' '}
            <Tooltip content={t('nodes.rackTooltip')}>
              <HelpIcon />
            </Tooltip>
          </Th>
          <Th sort={getSortParams('nodePool')}>{t('nodes.nodePool')}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {nodes.map((node, rowIndex) => {
          const isExpanded = expandedRows.has(node.id);
          const diskCapacity = node.attributes.storageCapacity;
          const diskUsage = node.attributes.storageUsed;
          const usedCapacity =
            diskUsage !== undefined && diskUsage !== null &&
            diskCapacity !== undefined && diskCapacity !== null
              ? diskUsage / diskCapacity
              : undefined;

          return (
            <Fragment key={node.id}>
              <Tr style={{ verticalAlign: 'middle' }}>
                <Td
                  expand={{
                    rowIndex,
                    isExpanded,
                    onToggle: () => toggleRowExpansion(node.id),
                  }}
                />
                <Td dataLabel={t('nodes.nodeId')} modifier="nowrap">
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
                </Td>
                <Td dataLabel={t('nodes.roles')} modifier="nowrap">
                  {node.attributes.roles?.map((role) => (
                    <div key={role}>{roleLabels[role].label}</div>
                  ))}
                </Td>
                <Td dataLabel={t('nodes.status')} modifier="nowrap">
                  <div className="pf-v6-u-active-color-100">
                    {node.attributes.broker && brokerStatusLabels[node.attributes.broker.status]}
                  </div>
                  <div>
                    {node.attributes.controller &&
                      controllerStatusLabels[node.attributes.controller.status]}
                  </div>
                </Td>
                <Td dataLabel={t('nodes.replicas')} modifier="fitContent" style={{ textAlign: 'right' }}>
                  {typeof node.attributes.broker?.leaderCount === 'number' &&
                  typeof node.attributes.broker?.replicaCount === 'number'
                    ? formatNumber(
                        node.attributes.broker.leaderCount + node.attributes.broker.replicaCount
                      )
                    : '-'}
                </Td>
                <Td dataLabel={t('nodes.rack')} modifier="nowrap">{node.attributes.rack || 'n/a'}</Td>
                <Td dataLabel={t('nodes.nodePool')} modifier="nowrap">{node.attributes.nodePool || 'n/a'}</Td>
              </Tr>
              {isExpanded && (
                <Tr isExpanded={isExpanded}>
                  <Td colSpan={7}>
                    <ExpandableRowContent>
                      <Flex gap={{ default: 'gap4xl' }} className="pf-v6-u-p-xl">
                        <FlexItem flex={{ default: 'flex_1' }} style={{ maxWidth: '50%' }}>
                          <Content>
                            <Content>
                              <strong>{t('nodes.hostName')}</strong>
                            </Content>
                            <Content>
                              <ClipboardCopy isReadOnly variant="expansion" isExpanded>
                                {node.attributes.host || 'n/a'}
                              </ClipboardCopy>
                            </Content>
                          </Content>
                        </FlexItem>
                        <FlexItem>
                          <Content>
                            <Content>
                              <strong>{t('nodes.diskUsage')}</strong>
                            </Content>
                          </Content>
                          <div>
                            {usedCapacity !== undefined && (
                              <div style={{ height: '300px', width: '230px' }}>
                                <ChartDonutUtilization
                                  data={{
                                    x: 'Used capacity',
                                    y: usedCapacity * 100,
                                  }}
                                  labels={({ datum }: any) =>
                                    datum.x ? `${datum.x}: ${(datum.y).toFixed(1)}%` : null
                                  }
                                  legendData={[
                                    {
                                      name: `Used capacity: ${formatBytes(diskUsage!)}`,
                                    },
                                    {
                                      name: `Available: ${formatBytes(diskCapacity! - diskUsage!)}`,
                                    },
                                  ]}
                                  legendOrientation="vertical"
                                  legendPosition="bottom"
                                  padding={{
                                    bottom: 75,
                                    left: 20,
                                    right: 20,
                                    top: 20,
                                  }}
                                  title={`${(usedCapacity * 100).toFixed(1)}%`}
                                  subTitle={`of ${formatBytes(diskCapacity!)}`}
                                  thresholds={[{ value: 60 }, { value: 90 }]}
                                  height={300}
                                  width={230}
                                />
                              </div>
                            )}
                          </div>
                        </FlexItem>
                        <FlexItem>
                          <Content>
                            <Content>
                              <strong>{t('nodes.kafkaVersion')}</strong>
                            </Content>
                          </Content>
                          <div>{node.attributes.kafkaVersion ?? 'Unknown'}</div>
                        </FlexItem>
                      </Flex>
                    </ExpandableRowContent>
                  </Td>
                </Tr>
              )}
            </Fragment>
          );
        })}
      </Tbody>
    </Table>
  );
}
