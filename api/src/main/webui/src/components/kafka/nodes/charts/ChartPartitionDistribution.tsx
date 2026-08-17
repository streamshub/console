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
import { useMemo } from 'react';

interface ChartPartitionDistributionProps {
  nodes: Node[];
}

export function ChartPartitionDistribution({ nodes }: ChartPartitionDistributionProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const brokerNodes = useMemo(() => nodes
    .filter((n) => n.attributes.broker != null)
    .sort((n1, n2) => parseInt(n2.id) - parseInt(n1.id)),
    [nodes]);

  // Bottom segment: leader partitions
  const leadersData = useMemo(() => brokerNodes.map((n) => {
    const broker = n.attributes.broker!;
    return {
      name: t('nodes.charts.partitionDistributionSeriesLeaders'),
      x: `Node ${n.id}`,
      y: broker.leaderCount,
      label: `${t('nodes.charts.partitionDistributionSeriesLeaders')}: ${formatNumber(broker.leaderCount)}`,
    };
  }), [t, brokerNodes]);

  // Top segment: follower replicas only (excludes leaders)
  const replicasData = useMemo(() => brokerNodes.map((n) => {
    const broker = n.attributes.broker!;
    return {
      name: t('nodes.charts.partitionDistributionSeriesReplicas'),
      x: `Node ${n.id}`,
      y: broker.replicaCount,
      label: `${t('nodes.charts.partitionDistributionSeriesReplicas')}: ${formatNumber(broker.replicaCount)}`,
    };
  }), [t, brokerNodes]);

  const legendData = [
    { name: t('nodes.charts.partitionDistributionSeriesReplicas') },
    { name: t('nodes.charts.partitionDistributionSeriesLeaders') },
  ];

  // Configure custom spacing dimensions
  const barWidth = 20;     // Thickness of each individual bar
  const innerPadding = 16;  // Distance between bars in pixels

  // Dynamically calculate the SVG canvas size based on data density
  const calculatedChartHeight = leadersData.length * (barWidth + innerPadding) + 100;
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 70, top: 40 };

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

  return (
    <div ref={containerRef} tabIndex={0}>
      <Chart
        ariaTitle={t('nodes.charts.partitionDistributionAriaTitle')}
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend orientation="horizontal" data={legendData} itemsPerRow={2} />
        }
        padding={padding}
        domainPadding={{ x: [30, 25] }}
        themeColor={ChartThemeColor.multiOrdered}
        width={width}
        height={calculatedChartHeight}
        legendAllowWrap={true}
      >
        <ChartAxis
          dependentAxis
          label="Partitions"
          showGrid
          horizontal
        />
        <ChartAxis
          dependentAxis
          orientation="top"
          horizontal
        />
        <ChartAxis />
        <ChartStack>
          <ChartBar
            data={replicasData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
          <ChartBar
            data={leadersData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
        </ChartStack>
      </Chart>
    </div>
  );
}
