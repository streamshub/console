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
import { VictoryZoomContainer } from 'victory-zoom-container';
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
      label: `${t('nodes.charts.partitionDistributionSeriesLeaders')}: ${formatNumber(broker.leaderCount)}`,
    };
  });

  // Top segment: follower replicas only (excludes leaders)
  const replicasData = brokerNodes.map((n) => {
    const broker = n.attributes.broker!;
    return {
      name: t('nodes.charts.partitionDistributionSeriesReplicas'),
      x: `Node ${n.id}`,
      y: broker.replicaCount,
      label: `${t('nodes.charts.partitionDistributionSeriesReplicas')}: ${formatNumber(broker.replicaCount)}`,
    };
  });

  const legendData = [
    { name: t('nodes.charts.partitionDistributionSeriesReplicas') },
    { name: t('nodes.charts.partitionDistributionSeriesLeaders') },
  ];

  const barWidth = 20;
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 90 };

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={t('nodes.charts.partitionDistributionAriaTitle')}
        containerComponent={
          <VictoryZoomContainer
            disable={brokerNodes.length < 21}
            zoomDimension="x"
            minimumZoom={{ x: 2 }}
          />
        }
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend orientation="horizontal" data={legendData} itemsPerRow={2} />
        }
        padding={padding}
        domainPadding={{ x: [30, 25] }}
        themeColor={ChartThemeColor.multiOrdered}
        width={width}
        legendAllowWrap={true}
      >
        <ChartAxis
          dependentAxis
          showGrid
          label="Partitions"
          style={{ axisLabel: { padding: 75 } }}
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
