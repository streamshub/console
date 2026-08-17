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
import { useMemo } from 'react';

interface ChartNodeStorageUsageProps {
  nodes: Node[];
}

export function ChartNodeStorageUsage({ nodes }: ChartNodeStorageUsageProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const storageNodes = useMemo(() => nodes
    .filter((n) => n.attributes.storageUsed != null && n.attributes.storageCapacity != null)
    .sort((n1, n2) => parseInt(n2.id) - parseInt(n1.id)),
    [nodes]);

  const usedData = useMemo(() => storageNodes.map((n) => {
    const capacity = n.attributes.storageCapacity as number;
    const used = n.attributes.storageUsed ?? 0;
    const usedPct = (used / capacity) * 100;

    return {
      name: t('nodes.charts.storageUsageSeriesUsed'),
      x: `Node ${n.id}`,
      y: n.attributes.storageUsed as number,
      label: t('nodes.charts.storageUsageSeriesUsedLabel', {
        "storageUsed": formatBytes(used),
        "storageUsedPct": usedPct.toFixed(2),
        "storageTotal": formatBytes(capacity),
      }),
    };
  }), [t, storageNodes]);

  const availableData = useMemo(() => storageNodes.map((n) => {
    const capacity = n.attributes.storageCapacity as number;
    const available = capacity - (n.attributes.storageUsed ?? 0);
    const availablePct = (available / capacity) * 100;

    return {
      name: t('nodes.charts.storageUsageSeriesAvailable'),
      x: `Node ${n.id}`,
      y: available,
      label: t('nodes.charts.storageUsageSeriesAvailableLabel', {
        "storageAvailable": formatBytes(available),
        "storageAvailablePct": availablePct.toFixed(2),
        "storageTotal": formatBytes(capacity),
      }),
    };
  }), [t, storageNodes]);

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

  // Configure custom spacing dimensions
  const barWidth = 20;     // Thickness of each individual bar
  const innerPadding = 16;  // Distance between bars in pixels

  // Dynamically calculate the SVG canvas size based on data density
  const calculatedChartHeight = usedData.length * (barWidth + innerPadding) + 100;
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 70 };

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

  return (
    <div ref={containerRef} tabIndex={0}>
      <Chart
        ariaTitle={t('nodes.charts.storageUsageAriaTitle')}
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
          label="Storage"
          showGrid
          tickValues={tickValues}
          tickFormat={(d: number) => formatBytes(d)}
          horizontal
        />
        <ChartAxis />
        <ChartStack
          labelComponent={<ChartTooltip constrainToVisibleArea />}
        >
          <ChartBar
            data={usedData}
            barWidth={barWidth}
          />
          <ChartBar
            data={availableData}
            barWidth={barWidth}
          />
        </ChartStack>
      </Chart>
    </div>
  );
}
