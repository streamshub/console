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
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from '@patternfly/react-icons';
import { useNodes } from '../api/hooks/useNodes';
import { useState, useRef, useEffect } from 'react';
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartStack,
  ChartVoronoiContainer,
  ChartThemeColor,
  ChartLegend,
} from '@patternfly/react-charts/victory';
import { formatNumber } from '../utils/format';
import { NodesTable } from '../components/NodesTable';

type DistributionFilter = 'all' | 'leaders' | 'followers';

export function NodesOverviewTab() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  
  // Fetch nodes for distribution chart (all nodes)
  const { data: chartData, isLoading: chartLoading, error: chartError } = useNodes(kafkaId, {
    pageSize: 100,
  });

  // State for table pagination, sorting, and filtering
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(20);
  const [sortBy, setSortBy] = useState<string>('id');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');
  const [filterNodePools, setFilterNodePools] = useState<string[]>([]);
  const [filterRoles, setFilterRoles] = useState<string[]>([]);
  const [filterBrokerStatuses, setFilterBrokerStatuses] = useState<string[]>([]);
  const [filterControllerStatuses, setFilterControllerStatuses] = useState<string[]>([]);

  // Fetch nodes for table with pagination and filters
  const { data: tableData, isLoading: tableLoading } = useNodes(kafkaId, {
    pageSize: perPage,
    pageCursor: page > 1 ? `after:page${page}` : undefined,
    sort: sortDir === 'desc' ? `-${sortBy}` : sortBy,
    nodePool: filterNodePools.length > 0 ? filterNodePools : undefined,
    roles: filterRoles.length > 0 ? filterRoles as any : undefined,
    brokerStatus: filterBrokerStatuses.length > 0 ? filterBrokerStatuses as any : undefined,
    controllerStatus: filterControllerStatuses.length > 0 ? filterControllerStatuses as any : undefined,
  });

  const [filter, setFilter] = useState<DistributionFilter>('all');
  const [chartWidth, setChartWidth] = useState(600);
  const chartContainerRef = useRef<HTMLDivElement>(null);

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

  // Handle page change
  const handlePageChange = (newPage: number, newPerPage: number) => {
    setPage(newPage);
    setPerPage(newPerPage);
  };

  // Handle sort change
  const handleSortChange = (column: string, direction: 'asc' | 'desc') => {
    setSortBy(column);
    setSortDir(direction);
  };

  // Handle filter changes
  const handleFilterChange = (
    nodePools: string[],
    roles: string[],
    brokerStatuses: string[],
    controllerStatuses: string[]
  ) => {
    setFilterNodePools(nodePools);
    setFilterRoles(roles);
    setFilterBrokerStatuses(brokerStatuses);
    setFilterControllerStatuses(controllerStatuses);
    setPage(1); // Reset to first page when filters change
  };

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

  const nodes = chartData?.data || [];
  const summary = chartData?.meta?.summary;
  const leadControllerId = summary?.leaderId || '';

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
              <NodesTable
                nodes={tableData?.data || []}
                nodePools={summary?.nodePools}
                statuses={summary?.statuses}
                leadControllerId={summary?.leaderId}
                isLoading={tableLoading}
                page={page}
                perPage={perPage}
                totalItems={tableData?.meta?.page?.total || 0}
                onPageChange={handlePageChange}
                onSortChange={handleSortChange}
                onFilterChange={handleFilterChange}
                sortBy={sortBy}
                sortDir={sortDir}
              />
            </CardBody>
          </Card>
        </GridItem>
      </Grid>
    </PageSection>
  );
}