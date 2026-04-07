/**
 * Incoming/Outgoing Bytes Chart Component
 * 
 * Displays topic byte rates over time with:
 * - Dual-line chart showing incoming and outgoing bytes
 * - Outgoing bytes displayed as negative values (mirrored)
 * - Interactive tooltips with formatted byte values
 * - Responsive width and legend
 */

import { useTranslation } from 'react-i18next';
import {
  Chart,
  ChartArea,
  ChartAxis,
  ChartGroup,
  ChartLegend,
  ChartLegendTooltip,
  ChartThemeColor,
  createContainer,
} from '@patternfly/react-charts/victory';
import { Alert } from '@patternfly/react-core';
import { formatBytes } from '../../../utils/format';
import { formatDateTime } from '../../../utils/dateTime';
import { useChartWidth } from '../utils/useChartWidth';
import { getHeight, getPadding } from '../utils/chartConsts';
import { DurationOptions, TimeSeriesMetrics, ChartDatum } from '../utils/types';

interface ChartIncomingOutgoingProps {
  incoming: TimeSeriesMetrics;
  outgoing: TimeSeriesMetrics;
  isVirtualKafkaCluster: boolean;
  selectedTopicName?: string;
  duration: DurationOptions;
}

export function ChartIncomingOutgoing({
  incoming,
  outgoing,
  isVirtualKafkaCluster,
  selectedTopicName,
  duration,
}: ChartIncomingOutgoingProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();

  const itemsPerRow = width > 500 ? 2 : 1;

  const showDate = duration >= DurationOptions.Last24hours;
  const axisFormat = showDate ? "HH:mm'\n'MMM dd" : 'HH:mm';
  const tooltipFormat = showDate ? 'MMM dd, HH:mm' : 'HH:mm';

  const hasMetrics =
    Object.keys(incoming).length > 0 && Object.keys(outgoing).length > 0;
  if (!hasMetrics || isVirtualKafkaCluster) {
    return (
      <Alert
        variant={isVirtualKafkaCluster ? 'info' : 'warning'}
        isInline
        isPlain
        title={
          isVirtualKafkaCluster
            ? t('ClusterChartsCard.virtual_cluster_metrics_unavailable')
            : t('ChartIncomingOutgoing.data_unavailable')
        }
      />
    );
  }

  const CursorVoronoiContainer = createContainer('voronoi', 'cursor');

  const incomingLabel = selectedTopicName
    ? `Incoming bytes (${selectedTopicName})`
    : 'Incoming bytes (all topics)';

  const outgoingLabel = selectedTopicName
    ? `Outgoing bytes (${selectedTopicName})`
    : 'Outgoing bytes (all topics)';

  const legendData = [
    {
      name: incomingLabel,
      childName: 'incoming',
    },
    {
      name: outgoingLabel,
      childName: 'outgoing',
    },
  ];

  const padding = getPadding(legendData.length / itemsPerRow);

  return (
    <div ref={containerRef}>
      <Chart
        ariaTitle="Topics bytes incoming and outgoing"
        containerComponent={
          <CursorVoronoiContainer
            cursorDimension="x"
            voronoiDimension="x"
            mouseFollowTooltips
            labelComponent={
              <ChartLegendTooltip
                legendData={legendData}
                title={(args: any) =>
                  formatDateTime({ value: args?.x ?? 0, format: tooltipFormat })
                }
              />
            }
            labels={({ datum }: { datum: ChartDatum }) => {
              return datum.value !== null
                ? formatBytes(datum.value)
                : 'no data';
            }}
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
          tickCount={4}
          orientation="bottom"
          offsetY={padding.bottom}
          style={{
            tickLabels: {
              padding: showDate ? 0 : 10,
            },
          }}
        />
        <ChartAxis
          dependentAxis
          tickFormat={(d: any) => formatBytes(Math.abs(d))}
        />
        <ChartGroup>
          <ChartArea
            key="incoming-line"
            data={Object.entries(incoming).map(([k, v]) => ({
              name: 'Incoming',
              x: Date.parse(k),
              y: v,
              value: v,
            }))}
            name="incoming"
            interpolation="linear"
          />
          <ChartArea
            key="outgoing-line"
            data={Object.entries(outgoing).map(([k, v]) => ({
              name: 'Outgoing',
              x: Date.parse(k),
              y: v * -1, // Mirror outgoing below the axis
              value: v,
            }))}
            name="outgoing"
            interpolation="linear"
          />
        </ChartGroup>
      </Chart>
    </div>
  );
}