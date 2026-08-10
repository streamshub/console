import { useTranslation } from 'react-i18next';
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartLegend,
  ChartStack,
  ChartThemeColor,
  ChartTooltip,
} from '@patternfly/react-charts/victory';
import { Alert } from '@patternfly/react-core';
import { Node } from '@/api/types';
import { formatNumber } from '@/utils/format';
import { useChartWidth } from '@/components/kafka/overview/utils/useChartWidth';
import { getPadding } from '@/components/kafka/overview/utils/chartConsts';

interface ChartPartitionDistributionProps {
  nodes: Node[];
}

export function ChartPartitionDistribution({ nodes }: ChartPartitionDistributionProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const brokerNodes = nodes.filter((n) => n.attributes.broker != null)
    .sort((n1, n2) => parseInt(n2.id) - parseInt(n1.id));

  if (brokerNodes.length === 0) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t('nodes.charts.partitionDistributionNoData')}
      />
    );
  }

  // Bottom segment: leader partitions
  const leadersData = brokerNodes.map((n) => {
    const broker = n.attributes.broker!;
    return {
      name: t('nodes.charts.partitionDistributionSeriesLeaders'),
      x: `Node ${n.id}`,
      y: broker.leaderCount,
      label: `Node ${n.id}\n${t('nodes.charts.partitionDistributionSeriesLeaders')}: ${formatNumber(broker.leaderCount)}`,
    };
  });

  // Top segment: follower replicas only (excludes leaders)
  const replicasData = brokerNodes.map((n) => {
    const broker = n.attributes.broker!;
    return {
      name: t('nodes.charts.partitionDistributionSeriesReplicas'),
      x: `Node ${n.id}`,
      y: broker.replicaCount,
      label: `Node ${n.id}\n${t('nodes.charts.partitionDistributionSeriesReplicas')}: ${formatNumber(broker.replicaCount)}`,
    };
  });

  const legendData = [
    { name: t('nodes.charts.partitionDistributionSeriesLeaders') },
    { name: t('nodes.charts.partitionDistributionSeriesReplicas') },
  ];

  const barWidth = 20;
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 80 };
  // Each node row gets 60px; top/bottom padding keeps outer bars off the edge.
  const chartHeight = brokerNodes.length * 60 + padding.top + padding.bottom;

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={t('nodes.charts.partitionDistributionAriaTitle')}
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend orientation="horizontal" data={legendData} itemsPerRow={2} />
        }
        height={chartHeight}
        padding={padding}
        domainPadding={{ x: [30, 25] }}
        themeColor={ChartThemeColor.multiOrdered}
        width={width}
        legendAllowWrap={true}
      >
        <ChartAxis dependentAxis showGrid />
        <ChartAxis />
        <ChartStack horizontal>
          <ChartBar
            data={leadersData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
          <ChartBar
            data={replicasData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
        </ChartStack>
      </Chart>
    </div>
  );
}
