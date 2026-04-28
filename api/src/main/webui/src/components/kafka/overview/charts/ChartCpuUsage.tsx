/**
 * CPU Usage Chart Component
 * 
 * Displays CPU usage over time for Kafka brokers with:
 * - Stacked area chart showing CPU usage per node
 * - Interactive tooltips with formatted values (in millicores)
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
import { formatDateTime } from '@/utils/dateTime';
import { useChartWidth } from '@/components/kafka/overview/utils/useChartWidth';
import { getHeight, getPadding } from '@/components/kafka/overview/utils/chartConsts';
import { DurationOptions, TimeSeriesMetrics, ChartDatum } from '@/components/kafka/overview/utils/types';

interface ChartLegendTitleDatum {
  x?: number;
}

interface ChartCpuUsageProps {
  usages: Record<string, TimeSeriesMetrics>;
  duration: DurationOptions;
}

export function ChartCpuUsage({ usages, duration }: ChartCpuUsageProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const showDate = duration >= DurationOptions.Last24hours;
  const axisFormat = showDate ? "HH:mm'\n'MMM dd" : 'HH:mm';
  const tooltipFormat = showDate ? 'MMM dd, HH:mm' : 'HH:mm';

  let itemsPerRow;
  if (width > 650) {
    itemsPerRow = 6;
  } else if (width > 300) {
    itemsPerRow = 3;
  } else {
    itemsPerRow = 1;
  }

  const hasMetrics = Object.keys(usages).length > 0;
  if (!hasMetrics) {
    return (
      <Alert
        variant="warning"
        isInline
        isPlain
        title={t('ChartCpuUsage.data_unavailable')}
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
        ariaTitle="CPU usage"
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                flyoutWidth={250}
                title={(args?: ChartLegendTitleDatum) =>
                  formatDateTime({
                    value: args?.x ?? 0,
                    format: tooltipFormat,
                  })
                }
              />
            }
            labels={({ datum }: { datum: ChartDatum }) =>
              datum.y !== null
                ? `${(datum.y * 1000).toFixed(0)}m`
                : 'no data'
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
          tickFormat={(d: number) =>
            formatDateTime({
              value: d,
              format: axisFormat,
            })
          }
          tickCount={5}
          style={{
            tickLabels: {
              padding: showDate ? 0 : 10,
            },
          }}
        />
        <ChartAxis
          dependentAxis
          showGrid={true}
          tickFormat={(d: number) => `${(d * 1000).toFixed(0)}m`}
        />
        <ChartStack>
          {Object.entries(usages).map(([nodeId, series]) => {
            return (
              <ChartArea
                key={`cpu-usage-${nodeId}`}
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