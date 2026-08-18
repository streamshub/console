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
import {
  Alert,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from '@patternfly/react-core';
import { Node } from '@/api/types';
import { formatBytes } from '@/utils/format';
import { useChartWidth } from '@/components/kafka/overview/utils/useChartWidth';
import { getPadding } from '@/components/kafka/overview/utils/chartConsts';
import { useMemo, useState } from 'react';

type StorageSortKey = 'nodeId' | 'usedAsc' | 'usedDesc' | 'availableAsc' | 'availableDesc';

interface ChartNodeStorageUsageProps {
  nodes: Node[];
}

export function ChartNodeStorageUsage({ nodes }: ChartNodeStorageUsageProps) {
  const { t } = useTranslation();
  const [containerRef, width] = useChartWidth();
  const [sortKey, setSortKey] = useState<StorageSortKey>('nodeId');
  const [isSortOpen, setIsSortOpen] = useState(false);

  const storageNodes = useMemo(() => {
    const filtered = nodes.filter(
      (n) => n.attributes.storageUsed != null && n.attributes.storageCapacity != null,
    );
    // Victory renders horizontal bar charts bottom-to-top, so the array must be
    // in the opposite order to what the user sees visually (top-to-bottom).
    return filtered.sort((n1, n2) => {
      const used1 = n1.attributes.storageUsed as number;
      const used2 = n2.attributes.storageUsed as number;
      const avail1 = (n1.attributes.storageCapacity as number) - used1;
      const avail2 = (n2.attributes.storageCapacity as number) - used2;
      switch (sortKey) {
        case 'usedAsc':      return used2 - used1;
        case 'usedDesc':     return used1 - used2;
        case 'availableAsc': return avail2 - avail1;
        case 'availableDesc':return avail1 - avail2;
        default:             return parseInt(n2.id) - parseInt(n1.id);
      }
    });
  }, [nodes, sortKey]);

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

  // Size the canvas so Victory allocates exactly (barWidth + innerPadding) px per bar slot.
  // Adding the actual top + bottom padding (rather than an arbitrary constant) ensures the
  // gap between bars stays constant regardless of the number of nodes.
  const legendRows = 1;
  const padding = { ...getPadding(legendRows), left: 70, top: 40 };
  const slotHeight = barWidth + innerPadding;
  const calculatedChartHeight = usedData.length * slotHeight + padding.top + padding.bottom;
  // Half a slot keeps the first/last bar the same distance from the axis edge as the
  // inter-bar gap, regardless of how many nodes are shown.
  const edgePadding = slotHeight / 2;

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
    <>
      <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 'var(--pf-t--global--spacer--sm)', marginBottom: 'var(--pf-t--global--spacer--sm)', minWidth: 0 }}>
        <span>{t('nodes.charts.sortBy.label')}</span>
        <Select
          isOpen={isSortOpen}
          onOpenChange={setIsSortOpen}
          onSelect={(_e, val) => {
            setSortKey(val as StorageSortKey);
            setIsSortOpen(false);
          }}
          popperProps={{ position: 'right', enableFlip: true }}
          toggle={(ref) => (
            <MenuToggle
              ref={ref}
              onClick={() => setIsSortOpen((o) => !o)}
              isExpanded={isSortOpen}
            >
              {t(`nodes.charts.sortBy.${sortKey}`)}
            </MenuToggle>
          )}
        >
          <SelectList>
            <SelectOption value="nodeId">
              {t('nodes.charts.sortBy.nodeId')}
            </SelectOption>
            <SelectOption value="usedAsc">
              {t('nodes.charts.sortBy.usedAsc')}
            </SelectOption>
            <SelectOption value="usedDesc">
              {t('nodes.charts.sortBy.usedDesc')}
            </SelectOption>
            <SelectOption value="availableAsc">
              {t('nodes.charts.sortBy.availableAsc')}
            </SelectOption>
            <SelectOption value="availableDesc">
              {t('nodes.charts.sortBy.availableDesc')}
            </SelectOption>
          </SelectList>
        </Select>
      </div>
      <div ref={containerRef} tabIndex={0}>
      <Chart
        ariaTitle={t('nodes.charts.storageUsageAriaTitle')}
        legendPosition="bottom-left"
        legendComponent={
          <ChartLegend orientation="horizontal" data={legendData} itemsPerRow={2} />
        }
        padding={padding}
        domainPadding={{ x: [edgePadding, edgePadding] }}
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
        <ChartAxis
          dependentAxis
          orientation="top"
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
    </>
  );
}
