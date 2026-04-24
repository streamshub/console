/**
 * Nodes Overview Tab - Shows node distribution chart and nodes table
 */

import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Grid,
  GridItem,
  Card,
  CardBody,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Tooltip,
  Icon,
  EmptyState,
  EmptyStateBody,
  Spinner,
  Title,
  CardHeader,
  CardTitle,
  ToggleGroup,
  ToggleGroupItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  ToolbarGroup,
  Pagination,
  PaginationVariant,
  Select,
  SelectOption,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  Flex,
  FlexItem,
  Label,
  Button,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
  FilterIcon,
} from '@patternfly/react-icons';
import { useNodes } from '@/api/hooks/useNodes';
import { useState, useRef, useEffect, useMemo } from 'react';
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartStack,
  ChartVoronoiContainer,
  ChartThemeColor,
} from '@patternfly/react-charts/victory';
import { formatNumber } from '@/utils/format';
import { NodesTable } from '@/components/kafka/nodes/NodesTable';
import { useTableState } from '@/hooks';
import type { BrokerStatus, ControllerStatus, NodeRoles } from '@/api/types';
import {
  useBrokerStatusLabels,
  useControllerStatusLabels,
  useRoleLabels,
} from '@/components/kafka/nodes/NodeStatusLabel';

type DistributionFilter = 'all' | 'leaders' | 'followers';

export function NodesOverviewTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  
  // Fetch nodes for distribution chart (all nodes)
  const { data: chartData, isLoading: chartLoading, error: chartError } = useNodes(kafkaId, {
    pageSize: 100,
  });

  // Table state (pagination + sorting)
  const table = useTableState<string>({
    initialSortColumn: 'id',
    initialSortDirection: 'asc',
  });

  // Filter state
  const [filterNodePools, setFilterNodePools] = useState<string[]>([]);
  const [filterRoles, setFilterRoles] = useState<NodeRoles[]>([]);
  const [filterBrokerStatuses, setFilterBrokerStatuses] = useState<BrokerStatus[]>([]);
  const [filterControllerStatuses, setFilterControllerStatuses] = useState<ControllerStatus[]>([]);

  // Filter menu states
  const [nodePoolFilterOpen, setNodePoolFilterOpen] = useState(false);
  const [roleFilterOpen, setRoleFilterOpen] = useState(false);
  const [statusFilterOpen, setStatusFilterOpen] = useState(false);

  // Fetch nodes for table with pagination and filters
  const { data: tableData, isLoading: tableLoading } = useNodes(kafkaId, {
    pageSize: table.pageSize,
    pageCursor: table.pageCursor,
    sort: table.sortBy,
    sortDir: table.sortDirection,
    nodePool: filterNodePools.length > 0 ? filterNodePools : undefined,
    roles: filterRoles.length > 0 ? filterRoles as any : undefined,
    brokerStatus: filterBrokerStatuses.length > 0 ? filterBrokerStatuses as any : undefined,
    controllerStatus: filterControllerStatuses.length > 0 ? filterControllerStatuses as any : undefined,
  });

  // Update table state when data changes
  useEffect(() => {
    table.setData(tableData);
  }, [tableData, table]);

  const [filter, setFilter] = useState<DistributionFilter>('all');
  const [chartWidth, setChartWidth] = useState(600);
  const chartContainerRef = useRef<HTMLDivElement>(null);

  // Get labels
  const roleLabels = useRoleLabels(chartData?.meta?.summary?.statuses);
  const brokerStatusLabels = useBrokerStatusLabels();
  const controllerStatusLabels = useControllerStatusLabels();

  // Update chart width on resize
  useEffect(() => {
    const updateWidth = () => {
      if (chartContainerRef.current) {
        setChartWidth(chartContainerRef.current.offsetWidth);
      }
    };

    updateWidth();
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);

  const nodes = chartData?.data || [];
  const summary = chartData?.meta?.summary;
  const leadControllerId = summary?.leaderId || '';
  const totalItems = tableData?.meta?.page?.total || 0;
  const currentPage = tableData?.meta?.page?.pageNumber || 1;

  // Calculate node counts
  const totalNodes = Object.values(summary?.statuses?.combined || {}).reduce(
    (sum, count) => sum + count,
    0
  );

  const brokersTotal = Object.values(summary?.statuses?.brokers || {}).reduce(
    (sum, count) => sum + count,
    0
  );

  const brokersWarning = Object.keys(summary?.statuses?.brokers || {}).some(
    (key) => key !== 'Running'
  );

  const controllersTotal = Object.values(summary?.statuses?.controllers || {}).reduce(
    (sum, count) => sum + count,
    0
  );

  const controllersWarning = Object.keys(summary?.statuses?.controllers || {}).some(
    (key) => key !== 'QuorumLeader' && key !== 'QuorumFollower'
  );

  // Build distribution data from broker nodes
  const distributionData: Record<string, { leaders: number; followers: number }> = {};
  nodes
    .filter((n) => n.attributes.roles?.includes('broker'))
    .forEach((node) => {
      distributionData[node.id] = {
        leaders: node.attributes.broker?.leaderCount || 0,
        followers: node.attributes.broker?.replicaCount || 0,
      };
    });

  const allCount = Object.values(distributionData).reduce(
    (acc, v) => v.followers + v.leaders + acc,
    0
  );
  const leadersCount = Object.values(distributionData).reduce(
    (acc, v) => v.leaders + acc,
    0
  );
  const followersCount = Object.values(distributionData).reduce(
    (acc, v) => v.followers + acc,
    0
  );

  const getCount = (nodeData: { leaders: number; followers: number }) => {
    switch (filter) {
      case 'leaders':
        return nodeData.leaders;
      case 'followers':
        return nodeData.followers;
      default:
        return nodeData.leaders + nodeData.followers;
    }
  };

  const getPercentage = (count: number) => {
    const total = filter === 'leaders' ? leadersCount : filter === 'followers' ? followersCount : allCount;
    return total > 0 ? ((count / total) * 100).toFixed(2) : '0.00';
  };

  // Node pool options for filter
  const nodePoolOptions = useMemo(() => {
    if (!summary?.nodePools) return [];
    return Object.entries(summary.nodePools).map(([poolName, poolMeta]) => ({
      value: poolName,
      label: poolName,
      count: poolMeta.count,
      description: `Roles: ${poolMeta.roles.join(', ')}`,
    }));
  }, [summary?.nodePools]);

  // Role options for filter
  const roleOptions: { value: NodeRoles; label: string; count: number }[] = [
    {
      value: 'broker',
      label: t('nodes.nodeRoles.broker'),
      count: summary?.statuses?.brokers
        ? Object.values(summary.statuses.brokers).reduce((sum, count) => sum + count, 0)
        : 0,
    },
    {
      value: 'controller',
      label: t('nodes.nodeRoles.controller'),
      count: summary?.statuses?.controllers
        ? Object.values(summary.statuses.controllers).reduce((sum, count) => sum + count, 0)
        : 0,
    },
  ];

  // Status options for filter (grouped)
  const brokerStatusOptions = useMemo(() => {
    if (!summary?.statuses?.brokers) return [];
    return Object.keys(brokerStatusLabels).map((status) => ({
      value: status as BrokerStatus,
      label: status,
      count: summary.statuses.brokers[status as BrokerStatus] || 0,
    }));
  }, [summary?.statuses, brokerStatusLabels]);

  const controllerStatusOptions = useMemo(() => {
    if (!summary?.statuses?.controllers) return [];
    return Object.keys(controllerStatusLabels).map((status) => ({
      value: status as ControllerStatus,
      label: status,
      count: summary.statuses.controllers[status as ControllerStatus] || 0,
    }));
  }, [summary?.statuses, controllerStatusLabels]);

  // Clear all filters
  const clearAllFilters = () => {
    setFilterNodePools([]);
    setFilterRoles([]);
    setFilterBrokerStatuses([]);
    setFilterControllerStatuses([]);
    table.resetPagination();
  };

  // Check if any filters are active
  const hasActiveFilters =
    filterNodePools.length > 0 ||
    filterRoles.length > 0 ||
    filterBrokerStatuses.length > 0 ||
    filterControllerStatuses.length > 0;

  if (chartLoading) {
    return (
      <PageSection>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h1" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (chartError) {
    return (
      <PageSection>
        <EmptyState>
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{chartError.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection isFilled>
      <Grid hasGutter>
        <GridItem md={3}>
          <Card>
            <CardBody>
              <DescriptionList isCompact isHorizontal>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t('nodes.distribution.totalNodes')}{' '}
                    <Tooltip content={t('nodes.distribution.totalNodesTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {formatNumber(totalNodes)}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t('nodes.distribution.controllerRole')}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {controllersWarning ? (
                      <Icon status="warning">
                        <ExclamationTriangleIcon />
                      </Icon>
                    ) : (
                      <Icon status="success">
                        <CheckCircleIcon />
                      </Icon>
                    )}
                    &nbsp; {formatNumber(controllersTotal)}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t('nodes.distribution.brokerRole')}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {brokersWarning ? (
                      <Icon status="warning">
                        <ExclamationTriangleIcon />
                      </Icon>
                    ) : (
                      <Icon status="success">
                        <CheckCircleIcon />
                      </Icon>
                    )}
                    &nbsp; {formatNumber(brokersTotal)}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t('nodes.distribution.leadController')}{' '}
                    <Tooltip content={t('nodes.distribution.leadControllerTooltip')}>
                      <HelpIcon />
                    </Tooltip>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    {t('nodes.distribution.leadControllerValue', {
                      leadController: leadControllerId,
                    })}
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </DescriptionList>
            </CardBody>
          </Card>
        </GridItem>
        <GridItem md={9}>
          <Card>
            <CardHeader>
              <CardTitle>
                {t('nodes.distribution.partitionsDistributionOfTotal')}{' '}
                <Tooltip content={t('nodes.distribution.partitionsDistributionOfTotalTooltip')}>
                  <HelpIcon />
                </Tooltip>
              </CardTitle>
            </CardHeader>
            {allCount > 0 ? (
              <CardBody>
                <ToggleGroup
                  isCompact
                  aria-label={t('nodes.distribution.distributionToggles')}
                >
                  <ToggleGroupItem
                    text={t('nodes.distribution.allLabel', { count: allCount })}
                    buttonId="toggle-all"
                    isSelected={filter === 'all'}
                    onChange={() => setFilter('all')}
                  />
                  <ToggleGroupItem
                    text={t('nodes.distribution.leadersLabel', { count: leadersCount })}
                    buttonId="toggle-leaders"
                    isSelected={filter === 'leaders'}
                    onChange={() => setFilter('leaders')}
                  />
                  <ToggleGroupItem
                    text={t('nodes.distribution.followersLabel', { count: followersCount })}
                    buttonId="toggle-followers"
                    isSelected={filter === 'followers'}
                    onChange={() => setFilter('followers')}
                  />
                </ToggleGroup>
                <div ref={chartContainerRef}>
                  <Chart
                    ariaDesc={t('nodes.distribution.distributionChartDescription')}
                    ariaTitle={t('nodes.distribution.distributionChartTitle')}
                    themeColor={ChartThemeColor.multiOrdered}
                    containerComponent={
                      <ChartVoronoiContainer
                        labels={({ datum }: any) => {
                          switch (filter) {
                            case 'followers':
                              return t('nodes.distribution.brokerNodeVoronoiFollowers', {
                                name: datum.name,
                                value: datum.y,
                              });
                            case 'leaders':
                              return t('nodes.distribution.brokerNodeVoronoiLeaders', {
                                name: datum.name,
                                value: datum.y,
                              });
                            default:
                              return t('nodes.distribution.brokerNodeVoronoiAll', {
                                name: datum.name,
                                value: datum.y,
                              });
                          }
                        }}
                        constrainToVisibleArea
                      />
                    }
                    legendOrientation="horizontal"
                    legendPosition="bottom"
                    legendData={Object.keys(distributionData).map((node) => {
                      const count = getCount(distributionData[node]);
                      const percentage = getPercentage(count);
                      return {
                        name: t('nodes.distribution.brokerNodeCount', {
                          node,
                          count,
                          percentage,
                        }),
                      };
                    })}
                    height={100}
                    padding={{
                      bottom: 70,
                      left: 0,
                      right: 0,
                      top: 30,
                    }}
                    width={chartWidth}
                  >
                    <ChartAxis
                      style={{
                        axis: { stroke: 'transparent' },
                        ticks: { stroke: 'transparent' },
                        tickLabels: { fill: 'transparent' },
                      }}
                    />
                    <ChartStack>
                      {Object.entries(distributionData).map(([node, data], idx) => (
                        <ChartBar
                          key={idx}
                          horizontal={true}
                          barWidth={15}
                          data={[
                            {
                              name: `Broker ${node}`,
                              x: 'x',
                              y: getCount(data),
                            },
                          ]}
                        />
                      ))}
                    </ChartStack>
                  </Chart>
                </div>
              </CardBody>
            ) : (
              <CardBody>
                <div>{t('nodes.distribution.metricsUnavailable')}</div>
              </CardBody>
            )}
          </Card>
        </GridItem>
        <GridItem>
          <Card>
            <CardHeader>
              <CardTitle>{t('nodes.title')}</CardTitle>
            </CardHeader>
            <CardBody>
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
                          const newPools = filterNodePools.includes(poolName)
                            ? filterNodePools.filter((p) => p !== poolName)
                            : [...filterNodePools, poolName];
                          setFilterNodePools(newPools);
                          table.resetPagination();
                        }}
                        toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                          <MenuToggle
                            ref={toggleRef}
                            onClick={() => setNodePoolFilterOpen(!nodePoolFilterOpen)}
                            isExpanded={nodePoolFilterOpen}
                            icon={<FilterIcon />}
                          >
                            {t('nodes.nodePool')}
                            {filterNodePools.length > 0 && ` (${filterNodePools.length})`}
                          </MenuToggle>
                        )}
                      >
                        <SelectList>
                          {nodePoolOptions.map((option) => (
                            <SelectOption
                              key={option.value}
                              value={option.value}
                              isSelected={filterNodePools.includes(option.value)}
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
                          const newRoles = filterRoles.includes(role)
                            ? filterRoles.filter((r) => r !== role)
                            : [...filterRoles, role];
                          setFilterRoles(newRoles);
                          table.resetPagination();
                        }}
                        toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                          <MenuToggle
                            ref={toggleRef}
                            onClick={() => setRoleFilterOpen(!roleFilterOpen)}
                            isExpanded={roleFilterOpen}
                            icon={<FilterIcon />}
                          >
                            {t('nodes.roles')}
                            {filterRoles.length > 0 && ` (${filterRoles.length})`}
                          </MenuToggle>
                        )}
                      >
                        <SelectList>
                          {roleOptions.map((option) => (
                            <SelectOption
                              key={option.value}
                              value={option.value}
                              isSelected={filterRoles.includes(option.value)}
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
                          let newBrokerStatuses = filterBrokerStatuses;
                          let newControllerStatuses = filterControllerStatuses;
                          
                          // Check if it's a broker status
                          if (brokerStatusOptions.some((opt) => opt.value === status)) {
                            newBrokerStatuses = filterBrokerStatuses.includes(status as BrokerStatus)
                              ? filterBrokerStatuses.filter((s) => s !== status)
                              : [...filterBrokerStatuses, status as BrokerStatus];
                            setFilterBrokerStatuses(newBrokerStatuses);
                          } else {
                            newControllerStatuses = filterControllerStatuses.includes(status as ControllerStatus)
                              ? filterControllerStatuses.filter((s) => s !== status)
                              : [...filterControllerStatuses, status as ControllerStatus];
                            setFilterControllerStatuses(newControllerStatuses);
                          }
                          
                          table.resetPagination();
                        }}
                        toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
                          <MenuToggle
                            ref={toggleRef}
                            onClick={() => setStatusFilterOpen(!statusFilterOpen)}
                            isExpanded={statusFilterOpen}
                            icon={<FilterIcon />}
                          >
                            {t('nodes.status')}
                            {(filterBrokerStatuses.length + filterControllerStatuses.length > 0) &&
                              ` (${filterBrokerStatuses.length + filterControllerStatuses.length})`}
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
                              isSelected={filterBrokerStatuses.includes(option.value)}
                              hasCheckbox
                            >
                              {brokerStatusLabels[option.value]}
                            </SelectOption>
                          ))}
                          <SelectOption isDisabled>
                            <strong>Controller</strong>
                          </SelectOption>
                          {controllerStatusOptions.map((option) => (
                            <SelectOption
                              key={option.value}
                              value={option.value}
                              isSelected={filterControllerStatuses.includes(option.value)}
                              hasCheckbox
                            >
                              {controllerStatusLabels[option.value]}
                            </SelectOption>
                          ))}
                        </SelectList>
                      </Select>
                    </ToolbarItem>
                  </ToolbarGroup>

                  <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                    <Pagination
                      itemCount={totalItems}
                      perPage={table.pageSize}
                      page={currentPage}
                      onSetPage={() => {}}
                      onPerPageSelect={table.handlePerPageChange}
                      onNextClick={table.handleNextPage}
                      onPreviousClick={table.handlePrevPage}
                      variant={PaginationVariant.top}
                      isCompact
                    />
                  </ToolbarItem>
                </ToolbarContent>

                {/* Filter chips */}
                {hasActiveFilters && (
                  <ToolbarContent>
                    <ToolbarGroup>
                      {filterNodePools.length > 0 && (
                        <ToolbarItem>
                          <strong>{t('nodes.nodePool')}:</strong>{' '}
                          {filterNodePools.map((pool) => (
                            <Label
                              key={pool}
                              color="blue"
                              onClose={() => {
                                const newPools = filterNodePools.filter((p) => p !== pool);
                                setFilterNodePools(newPools);
                                table.resetPagination();
                              }}
                              className="pf-v6-u-mr-sm"
                            >
                              {pool}
                            </Label>
                          ))}
                        </ToolbarItem>
                      )}
                      {filterRoles.length > 0 && (
                        <ToolbarItem>
                          <strong>{t('nodes.roles')}:</strong>{' '}
                          {filterRoles.map((role) => (
                            <Label
                              key={role}
                              color="blue"
                              onClose={() => {
                                const newRoles = filterRoles.filter((r) => r !== role);
                                setFilterRoles(newRoles);
                                table.resetPagination();
                              }}
                              className="pf-v6-u-mr-sm"
                            >
                              {roleLabels[role].label}
                            </Label>
                          ))}
                        </ToolbarItem>
                      )}
                      {filterBrokerStatuses.length > 0 && (
                        <ToolbarItem>
                          <strong>Broker Status:</strong>{' '}
                          {filterBrokerStatuses.map((status) => (
                            <Label
                              key={status}
                              color="blue"
                              onClose={() => {
                                const newBrokerStatuses = filterBrokerStatuses.filter((s) => s !== status);
                                setFilterBrokerStatuses(newBrokerStatuses);
                                table.resetPagination();
                              }}
                              className="pf-v6-u-mr-sm"
                            >
                              {status}
                            </Label>
                          ))}
                        </ToolbarItem>
                      )}
                      {filterControllerStatuses.length > 0 && (
                        <ToolbarItem>
                          <strong>Controller Status:</strong>{' '}
                          {filterControllerStatuses.map((status) => (
                            <Label
                              key={status}
                              color="blue"
                              onClose={() => {
                                const newControllerStatuses = filterControllerStatuses.filter((s) => s !== status);
                                setFilterControllerStatuses(newControllerStatuses);
                                table.resetPagination();
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

              <NodesTable
                kafkaId={kafkaId!}
                nodes={tableData?.data || []}
                isLoading={tableLoading}
                hasActiveFilters={hasActiveFilters}
                onClearAllFilters={clearAllFilters}
                onSort={table.handleSort}
                sortBy={table.sortBy}
                sortDirection={table.sortDirection}
              />
              {(tableData?.data || []).length > 0 && (
                <Toolbar>
                  <ToolbarContent>
                    <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
                      <Pagination
                        itemCount={totalItems}
                        perPage={table.pageSize}
                        page={currentPage}
                        onSetPage={() => {}}
                        onPerPageSelect={table.handlePerPageChange}
                        onNextClick={table.handleNextPage}
                        onPreviousClick={table.handlePrevPage}
                        variant={PaginationVariant.bottom}
                        isCompact
                      />
                    </ToolbarItem>
                  </ToolbarContent>
                </Toolbar>
              )}
            </CardBody>
          </Card>
        </GridItem>
      </Grid>
    </PageSection>
  );
}
