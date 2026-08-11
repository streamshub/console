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
import { formatBytes } from '@/utils/format';
import { useChartWidth } from '@/components/kafka/overview/utils/useChartWidth';
import { getPadding } from '@/components/kafka/overview/utils/chartConsts';
import { VictoryZoomContainer } from 'victory-zoom-container';

interface ChartNodeStorageUsageProps {
  nodes: Node[];
}

export function ChartNodeStorageUsage({ nodes }: ChartNodeStorageUsageProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const storageNodes = nodes.filter(
    (n) => n.attributes.storageUsed != null && n.attributes.storageCapacity != null,
  ).sort((n1, n2) => parseInt(n2.id) - parseInt(n1.id));

  if (storageNodes.length === 0) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t('nodes.charts.storageUsageNoData')}
      />
    );
  }

  const usedData = storageNodes.map((n) => ({
    name: t('nodes.charts.storageUsageSeriesUsed'),
    x: `Node ${n.id}`,
    y: n.attributes.storageUsed as number,
    label: `Node ${n.id}\n${t('nodes.charts.storageUsageSeriesUsed')}: ${formatBytes(n.attributes.storageUsed as number)}`,
  }));

  const availableData = storageNodes.map((n) => {
    const available = (n.attributes.storageCapacity as number) - (n.attributes.storageUsed as number);
    return {
      name: t('nodes.charts.storageUsageSeriesAvailable'),
      x: `Node ${n.id}`,
      y: available,
      label: `Node ${n.id}\n${t('nodes.charts.storageUsageSeriesAvailable')}: ${formatBytes(available)}`,
    };
  });

  const legendData = [
    { name: t('nodes.charts.storageUsageSeriesUsed') },
    { name: t('nodes.charts.storageUsageSeriesAvailable') },
  ];

  // Compute 5 evenly-spaced, round tick values from 0 to maxCapacity.
  // Victory's tickCount hint does not produce round values for byte ranges,
  // so we derive explicit tickValues instead.
  const maxCapacity = Math.max(...storageNodes.map((n) => n.attributes.storageCapacity as number));
  const tickStep = maxCapacity / 4;
  // Round step up to a power-of-1024 boundary so labels stay in one unit.
  const unitBoundary = Math.pow(1024, Math.floor(Math.log(tickStep) / Math.log(1024)));
  const roundedStep = Math.ceil(tickStep / unitBoundary) * unitBoundary;
  const tickValues = [0, 1, 2, 3, 4].map((i) => i * roundedStep);

  const barWidth = 20;
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 90 };

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle={t('nodes.charts.storageUsageAriaTitle')}
        containerComponent={
          <VictoryZoomContainer
            disable={storageNodes.length < 21}
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
          label="Storage"
          showGrid
          tickValues={tickValues}
          tickFormat={(d: number) => formatBytes(d)}
          style={{ axisLabel: { padding: 75 } }}
        />
        <ChartAxis />
        <ChartStack>
          <ChartBar
            data={usedData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
          <ChartBar
            data={availableData}
            barWidth={barWidth}
            labelComponent={<ChartTooltip constrainToVisibleArea />}
          />
        </ChartStack>
      </Chart>
    </div>
  );
}
