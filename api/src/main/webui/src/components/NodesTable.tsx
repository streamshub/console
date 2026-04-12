/**
 * Nodes Table Component
 * Displays Kafka cluster nodes with filtering, sorting, and expandable rows
 */

import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
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
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  ToolbarGroup,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  Pagination,
  EmptyState,
  EmptyStateBody,
  Title,
  Flex,
  FlexItem,
  Content,
  ClipboardCopy,
  Label,
  Tooltip,
  Button,
} from '@patternfly/react-core';
import { HelpIcon, FilterIcon } from '@patternfly/react-icons';
import { ChartDonutUtilization } from '@patternfly/react-charts/victory';
import type { Node, BrokerStatus, ControllerStatus, NodeRoles, Statuses, NodePools } from '../api/types';
import {
  useRoleLabels,
  useBrokerStatusLabels,
  useControllerStatusLabels,
  useBrokerStatusLabelsWithCount,
  useControllerStatusLabelsWithCount,
} from './NodeStatusLabel';
import { formatNumber } from '../utils/format';

interface NodesTableProps {
  nodes: Node[];
  nodePools?: NodePools;
  statuses?: Statuses;
  leadControllerId?: string;
  isLoading?: boolean;
  page: number;
  perPage: number;
  totalItems: number;
  onPageChange: (page: number, perPage: number) => void;
  onSortChange?: (sortBy: string, sortDir: 'asc' | 'desc') => void;
  onFilterChange?: (
    nodePools: string[],
    roles: string[],
    brokerStatuses: string[],
    controllerStatuses: string[]
  ) => void;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

type FilterType = 'nodePool' | 'role' | 'status';

export function NodesTable({
  nodes,
  nodePools,
  statuses,
  leadControllerId,
  isLoading,
  page,
  perPage,
  totalItems,
  onPageChange,
  onSortChange,
  onFilterChange,
  sortBy,
  sortDir = 'asc',
}: NodesTableProps) {
  const { t } = useTranslation();
  const roleLabels = useRoleLabels(statuses);
  const brokerStatusLabels = useBrokerStatusLabels();
  const controllerStatusLabels = useControllerStatusLabels();
  const brokerStatusLabelsWithCount = useBrokerStatusLabelsWithCount(statuses?.brokers);
  const controllerStatusLabelsWithCount = useControllerStatusLabelsWithCount(statuses?.controllers);

  // Filter states
  const [selectedNodePools, setSelectedNodePools] = useState<string[]>([]);
  const [selectedRoles, setSelectedRoles] = useState<NodeRoles[]>([]);
  const [selectedBrokerStatuses, setSelectedBrokerStatuses] = useState<BrokerStatus[]>([]);
  const [selectedControllerStatuses, setSelectedControllerStatuses] = useState<ControllerStatus[]>([]);

  // Filter menu states
  const [nodePoolFilterOpen, setNodePoolFilterOpen] = useState(false);
  const [roleFilterOpen, setRoleFilterOpen] = useState(false);
  const [statusFilterOpen, setStatusFilterOpen] = useState(false);

  // Expandable row state
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  // Notify parent of filter changes
  const notifyFilterChange = (
    pools: string[],
    roles: string[],
    brokerStats: string[],
    controllerStats: string[]
  ) => {
    if (onFilterChange) {
      onFilterChange(pools, roles, brokerStats, controllerStats);
    }
  };

  // Toggle row expansion
  const toggleRowExpanded = (nodeId: string) => {
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

  // Handle sorting
  const handleSort = (columnKey: string) => {
    if (!onSortChange) return;
    const newDir = sortBy === columnKey && sortDir === 'asc' ? 'desc' : 'asc';
    onSortChange(columnKey, newDir);
  };

  // Get sort params for column
  const getSortParams = (columnKey: string): ThProps['sort'] => ({
    sortBy: {
      index: sortBy === columnKey ? 0 : undefined,
      direction: sortDir,
    },
    onSort: () => handleSort(columnKey),
    columnIndex: 0,
  });

  // Format bytes
  const formatBytes = (bytes?: number): string => {
    if (bytes === undefined || bytes === null) return 'N/A';
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  // Node pool options for filter
  const nodePoolOptions = useMemo(() => {
    if (!nodePools) return [];
    return Object.entries(nodePools).map(([poolName, poolMeta]) => ({
      value: poolName,
      label: poolName,
      count: poolMeta.count,
      description: `Roles: ${poolMeta.roles.join(', ')}`,
    }));
  }, [nodePools]);

  // Role options for filter
  const roleOptions: { value: NodeRoles; label: string; count: number }[] = [
    {
      value: 'broker',
      label: t('nodes.nodeRoles.broker'),
      count: statuses?.brokers
        ? Object.values(statuses.brokers).reduce((sum, count) => sum + count, 0)
        : 0,
    },
    {
      value: 'controller',
      label: t('nodes.nodeRoles.controller'),
      count: statuses?.controllers
        ? Object.values(statuses.controllers).reduce((sum, count) => sum + count, 0)
        : 0,
    },
  ];

  // Status options for filter (grouped)
  const brokerStatusOptions = useMemo(() => {
    if (!statuses?.brokers) return [];
    return Object.keys(brokerStatusLabels).map((status) => ({
      value: status as BrokerStatus,
      label: status,
      count: statuses.brokers[status as BrokerStatus] || 0,
    }));
  }, [statuses, brokerStatusLabels]);

  const controllerStatusOptions = useMemo(() => {
    if (!statuses?.controllers) return [];
    return Object.keys(controllerStatusLabels).map((status) => ({
      value: status as ControllerStatus,
      label: status,
      count: statuses.controllers[status as ControllerStatus] || 0,
    }));
  }, [statuses, controllerStatusLabels]);

  // Clear all filters
  const clearAllFilters = () => {
    setSelectedNodePools([]);
    setSelectedRoles([]);
    setSelectedBrokerStatuses([]);
    setSelectedControllerStatuses([]);
    notifyFilterChange([], [], [], []);
  };

  // Check if any filters are active
  const hasActiveFilters =
    selectedNodePools.length > 0 ||
    selectedRoles.length > 0 ||
    selectedBrokerStatuses.length > 0 ||
    selectedControllerStatuses.length > 0;

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
        {hasActiveFilters && (
          <button onClick={clearAllFilters} className="pf-v6-c-button pf-m-primary">
            {t('topics.filter.clearAllFilters')}
          </button>
        )}
      </EmptyState>
    );
  }

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarGroup variant="filter-group">
            {/* Node Pool Filter */}
            <ToolbarItem>
              <Select
                isOpen={nodePoolFilterOpen}
                onOpenChange={setNodePoolFilterOpen}
                onSelect={(_, value) => {
                  const poolName = value as string;
                  const newPools = selectedNodePools.includes(poolName)
                    ? selectedNodePools.filter((p) => p !== poolName)
                    : [...selectedNodePools, poolName];
                  setSelectedNodePools(newPools);
                  notifyFilterChange(newPools, selectedRoles, selectedBrokerStatuses, selectedControllerStatuses);
                }}
                toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                  <MenuToggle
                    ref={toggleRef}
                    onClick={() => setNodePoolFilterOpen(!nodePoolFilterOpen)}
                    isExpanded={nodePoolFilterOpen}
                    icon={<FilterIcon />}
                  >
                    {t('nodes.nodePool')}
                    {selectedNodePools.length > 0 && ` (${selectedNodePools.length})`}
                  </MenuToggle>
                )}
              >
                <SelectList>
                  {nodePoolOptions.map((option) => (
                    <SelectOption
                      key={option.value}
                      value={option.value}
                      isSelected={selectedNodePools.includes(option.value)}
                      hasCheckbox
                    >
                      <Flex>
                        <FlexItem>{option.label}</FlexItem>
                        <FlexItem align={{ default: 'alignRight' }}>
                          <span style={{ color: 'var(--pf-t--global--text--color--subtle)' }}>
                            {option.count}
                          </span>
                        </FlexItem>
                      </Flex>
                      {option.description && (
                        <div style={{ fontSize: 'var(--pf-t--global--font--size--sm)' }}>
                          {option.description}
                        </div>
                      )}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </ToolbarItem>

            {/* Role Filter */}
            <ToolbarItem>
              <Select
                isOpen={roleFilterOpen}
                onOpenChange={setRoleFilterOpen}
                onSelect={(_, value) => {
                  const role = value as NodeRoles;
                  const newRoles = selectedRoles.includes(role)
                    ? selectedRoles.filter((r) => r !== role)
                    : [...selectedRoles, role];
                  setSelectedRoles(newRoles);
                  notifyFilterChange(selectedNodePools, newRoles, selectedBrokerStatuses, selectedControllerStatuses);
                }}
                toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                  <MenuToggle
                    ref={toggleRef}
                    onClick={() => setRoleFilterOpen(!roleFilterOpen)}
                    isExpanded={roleFilterOpen}
                    icon={<FilterIcon />}
                  >
                    {t('nodes.roles')}
                    {selectedRoles.length > 0 && ` (${selectedRoles.length})`}
                  </MenuToggle>
                )}
              >
                <SelectList>
                  {roleOptions.map((option) => (
                    <SelectOption
                      key={option.value}
                      value={option.value}
                      isSelected={selectedRoles.includes(option.value)}
                      hasCheckbox
                    >
                      <Flex>
                        <FlexItem>{option.label}</FlexItem>
                        <FlexItem align={{ default: 'alignRight' }}>
                          <span style={{ color: 'var(--pf-t--global--text--color--subtle)' }}>
                            {option.count}
                          </span>
                        </FlexItem>
                      </Flex>
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </ToolbarItem>

            {/* Status Filter (Grouped) */}
            <ToolbarItem>
              <Select
                isOpen={statusFilterOpen}
                onOpenChange={setStatusFilterOpen}
                onSelect={(_, value) => {
                  const status = value as BrokerStatus | ControllerStatus;
                  let newBrokerStatuses = selectedBrokerStatuses;
                  let newControllerStatuses = selectedControllerStatuses;
                  
                  // Check if it's a broker status
                  if (brokerStatusOptions.some((opt) => opt.value === status)) {
                    newBrokerStatuses = selectedBrokerStatuses.includes(status as BrokerStatus)
                      ? selectedBrokerStatuses.filter((s) => s !== status)
                      : [...selectedBrokerStatuses, status as BrokerStatus];
                    setSelectedBrokerStatuses(newBrokerStatuses);
                  } else {
                    newControllerStatuses = selectedControllerStatuses.includes(status as ControllerStatus)
                      ? selectedControllerStatuses.filter((s) => s !== status)
                      : [...selectedControllerStatuses, status as ControllerStatus];
                    setSelectedControllerStatuses(newControllerStatuses);
                  }
                  
                  notifyFilterChange(selectedNodePools, selectedRoles, newBrokerStatuses, newControllerStatuses);
                }}
                toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                  <MenuToggle
                    ref={toggleRef}
                    onClick={() => setStatusFilterOpen(!statusFilterOpen)}
                    isExpanded={statusFilterOpen}
                    icon={<FilterIcon />}
                  >
                    {t('nodes.status')}
                    {(selectedBrokerStatuses.length + selectedControllerStatuses.length > 0) &&
                      ` (${selectedBrokerStatuses.length + selectedControllerStatuses.length})`}
                  </MenuToggle>
                )}
              >
                <SelectList>
                  <SelectOption isDisabled>
                    <strong>Broker</strong>
                  </SelectOption>
                  {brokerStatusOptions.map((option) => (
                    <SelectOption
                      key={option.value}
                      value={option.value}
                      isSelected={selectedBrokerStatuses.includes(option.value)}
                      hasCheckbox
                    >
                      {brokerStatusLabelsWithCount[option.value]}
                    </SelectOption>
                  ))}
                  <SelectOption isDisabled>
                    <strong>Controller</strong>
                  </SelectOption>
                  {controllerStatusOptions.map((option) => (
                    <SelectOption
                      key={option.value}
                      value={option.value}
                      isSelected={selectedControllerStatuses.includes(option.value)}
                      hasCheckbox
                    >
                      {controllerStatusLabelsWithCount[option.value]}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </ToolbarItem>
          </ToolbarGroup>

          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalItems}
              page={page}
              perPage={perPage}
              onSetPage={(_, newPage) => onPageChange(newPage, perPage)}
              onPerPageSelect={(_, newPerPage) => onPageChange(1, newPerPage)}
              variant="top"
              isCompact
            />
          </ToolbarItem>
        </ToolbarContent>

        {/* Filter chips */}
        {hasActiveFilters && (
          <ToolbarContent>
            <ToolbarGroup>
              {selectedNodePools.length > 0 && (
                <ToolbarItem>
                  <strong>{t('nodes.nodePool')}:</strong>{' '}
                  {selectedNodePools.map((pool) => (
                    <Label
                      key={pool}
                      color="blue"
                      onClose={() => {
                        const newPools = selectedNodePools.filter((p) => p !== pool);
                        setSelectedNodePools(newPools);
                        notifyFilterChange(newPools, selectedRoles, selectedBrokerStatuses, selectedControllerStatuses);
                      }}
                      className="pf-v6-u-mr-sm"
                    >
                      {pool}
                    </Label>
                  ))}
                </ToolbarItem>
              )}
              {selectedRoles.length > 0 && (
                <ToolbarItem>
                  <strong>{t('nodes.roles')}:</strong>{' '}
                  {selectedRoles.map((role) => (
                    <Label
                      key={role}
                      color="blue"
                      onClose={() => {
                        const newRoles = selectedRoles.filter((r) => r !== role);
                        setSelectedRoles(newRoles);
                        notifyFilterChange(selectedNodePools, newRoles, selectedBrokerStatuses, selectedControllerStatuses);
                      }}
                      className="pf-v6-u-mr-sm"
                    >
                      {roleLabels[role].label}
                    </Label>
                  ))}
                </ToolbarItem>
              )}
              {(selectedBrokerStatuses.length > 0 || selectedControllerStatuses.length > 0) && (
                <ToolbarItem>
                  <strong>{t('nodes.status')}:</strong>{' '}
                  {selectedBrokerStatuses.map((status) => (
                    <Label
                      key={status}
                      color="blue"
                      onClose={() => {
                        const newBrokerStatuses = selectedBrokerStatuses.filter((s) => s !== status);
                        setSelectedBrokerStatuses(newBrokerStatuses);
                        notifyFilterChange(selectedNodePools, selectedRoles, newBrokerStatuses, selectedControllerStatuses);
                      }}
                      className="pf-v6-u-mr-sm"
                    >
                      {status}
                    </Label>
                  ))}
                  {selectedControllerStatuses.map((status) => (
                    <Label
                      key={status}
                      color="blue"
                      onClose={() => {
                        const newControllerStatuses = selectedControllerStatuses.filter((s) => s !== status);
                        setSelectedControllerStatuses(newControllerStatuses);
                        notifyFilterChange(selectedNodePools, selectedRoles, selectedBrokerStatuses, newControllerStatuses);
                      }}
                      className="pf-v6-u-mr-sm"
                    >
                      {status}
                    </Label>
                  ))}
                </ToolbarItem>
              )}
              <ToolbarItem>
                <Button variant="link" onClick={clearAllFilters}>
                  {t('topics.filter.clearAllFilters')}
                </Button>
              </ToolbarItem>
            </ToolbarGroup>
          </ToolbarContent>
        )}
      </Toolbar>

      <Table aria-label={t('nodes.title')} variant="compact">
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
              <>
                <Tr key={node.id} style={{ verticalAlign: 'middle' }}>
                  <Td
                    expand={{
                      rowIndex,
                      isExpanded,
                      onToggle: () => toggleRowExpanded(node.id),
                    }}
                    modifier="fitContent"
                  />
                  <Td dataLabel={t('nodes.nodeId')} modifier="truncate">
                    {node.attributes.broker ? (
                      <Link to={`../${node.id}`}>{node.id}</Link>
                    ) : (
                      <>{node.id}</>
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
              </>
            );
          })}
        </Tbody>
      </Table>

      <Toolbar>
        <ToolbarContent>
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={totalItems}
              page={page}
              perPage={perPage}
              onSetPage={(_, newPage) => onPageChange(newPage, perPage)}
              onPerPageSelect={(_, newPerPage) => onPageChange(1, newPerPage)}
              variant="bottom"
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </>
  );
}