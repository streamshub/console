/**
 * Memory Usage Chart Component
 * 
 * Displays memory usage over time for Kafka brokers with:
 * - Stacked area chart showing memory usage per node
 * - Interactive tooltips with formatted byte values
 * - Responsive width and legend
 */

import { useTranslation } from 'react-i18next';
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartLegend,
  ChartLegendTooltip,
  ChartStack,
  ChartThemeColor,
  createContainer,
} from '@patternfly/react-charts/victory';
import { Alert } from '@patternfly/react-core';
import { formatBytes } from '@/utils/format';
import { formatDateTime } from '@/utils/dateTime';
import { useChartWidth } from '@/components/kafka/overview/utils/useChartWidth';
import { getHeight, getPadding } from '@/components/kafka/overview/utils/chartConsts';
import { DurationOptions, TimeSeriesMetrics, ChartDatum } from '@/components/kafka/overview/utils/types';

interface ChartMemoryUsageProps {
  usages: Record<string, TimeSeriesMetrics>;
  duration: DurationOptions;
}

export function ChartMemoryUsage({ usages, duration }: ChartMemoryUsageProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const showDate = duration >= DurationOptions.Last24hours;
  const axisFormat = showDate ? "HH:mm'\n'MMM dd" : 'HH:mm';
  const tooltipFormat = showDate ? 'MMM dd, HH:mm' : 'HH:mm';

  const itemsPerRow = width > 650 ? 6 : width > 300 ? 3 : 1;

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t('ChartMemoryUsage.data_unavailable')}
      />
    );
  }

  const CursorVoronoiContainer = createContainer('voronoi', 'cursor');
  const legendData = Object.keys(usages).map((nodeId) => ({
    name: `Node ${nodeId}`,
    childName: `node ${nodeId}`,
  }));
  const padding = getPadding(legendData.length / itemsPerRow);

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle="Memory usage"
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                flyoutWidth={250}
                title={(args: any) =>
                  formatDateTime({ value: args?.x ?? 0, format: tooltipFormat })
                }
              />
            }
            labels={({ datum }: { datum: ChartDatum }) =>
              datum.y !== null ? formatBytes(datum.y) : 'no data'
            }
            constrainToVisibleArea
          />
        }
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend
            orientation="horizontal"
            data={legendData}
            itemsPerRow={itemsPerRow}
          />
        }
        height={getHeight(legendData.length / itemsPerRow)}
        padding={padding}
        themeColor={ChartThemeColor.multiUnordered}
        width={width}
        legendAllowWrap={true}
      >
        <ChartAxis
          scale="time"
          tickFormat={(d: any) => formatDateTime({ value: d, format: axisFormat })}
          style={{
            tickLabels: {
              padding: showDate ? 0 : 10,
            },
          }}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d: any) => formatBytes(d)}
        />
        <ChartStack>
          {Object.entries(usages).map(([nodeId, series]) => {
            return (
              <ChartArea
                key={`memory-usage-${nodeId}`}
                data={Object.entries(series).map(([k, v]) => ({
                  name: `Node ${nodeId}`,
                  x: Date.parse(k),
                  y: v,
                }))}
                name={`node ${nodeId}`}
              />
            );
          })}
        </ChartStack>
      </Chart>
    </div>
  );
}