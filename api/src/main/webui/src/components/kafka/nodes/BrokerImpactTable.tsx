import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Button,
  EmptyState,
  EmptyStateBody,
  Pagination,
  SearchInput,
  Select,
  SelectList,
  SelectOption,
  MenuToggle,
  Switch,
  Toolbar,
  ToolbarContent,
  ToolbarFilter,
  ToolbarGroup,
  ToolbarItem,
} from '@patternfly/react-core';
import { Table, Tbody, Td, Th, Thead, Tr, ThProps, InnerScrollContainer } from '@patternfly/react-table';
import { BrokerCapacity, BrokerLoadImpact } from '@/api/types';

interface BrokerImpactTableProps {
  brokerCapacity?: BrokerCapacity;
  brokerImpact: Record<string, Record<string, BrokerLoadImpact>> | null | undefined;
}

interface BrokerRow {
  brokerId: string;
  metrics: Record<string, BrokerLoadImpact>;
}

/**
 * Column groups shown in the table, in display order.
 * pctKey drives the bar fill and delta colouring.
 * absKey (optional) is shown alongside the percentage inside the bar label.
 */
const COLUMN_GROUPS: Array<{
  label: string;
  pctKey?: string;
  absKey?: string;
  absUnit?: string;
}> = [
  { label: 'Storage', pctKey: 'diskUsedPercentage', absKey: 'diskUsedMB', absUnit: 'MB' },
  { label: 'CPU',  pctKey: 'cpuPercentage' },
  { label: 'Leaders',  absKey: 'leaders' },
  { label: 'Followers',  absKey: 'replicas' },
  { label: 'Network In',  absKey: 'leaderNetworkInRateKB', absUnit: "KB/s" },
  { label: 'Network Out',  absKey: 'networkOutRateKB', absUnit: "KB/s" },
];

// Sortable column identifiers
type SortKey =
  | 'brokerId'
  | `${string}-before`
  | `${string}-after`
  | `${string}-delta`;

/** Bar with the value label centred inside it. */
function BarCell({
  pct,
  label,
}: {
  pct: number | null | undefined;
  label: string;
}) {
  if (pct == null) return <span>–</span>;

  const barColor = 'var(--pf-t--global--color--brand--default)';
  const fill = Math.min(100, Math.max(0, pct));

  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      minWidth: 130,
      width: '100%',
      height: 20,
      background: 'var(--pf-t--global--background--color--secondary--default)',
      borderRadius: 3,
      overflow: 'hidden',
      position: 'relative',
    }}>
      <span style={{
        position: 'absolute',
        left: 0,
        top: 0,
        width: `${fill}%`,
        height: '100%',
        background: barColor,
        borderRadius: 3,
      }} />
      <span style={{
        position: 'absolute',
        width: '100%',
        textAlign: 'center',
        fontSize: 'var(--pf-t--global--font--size--sm)',
        fontWeight: 500,
        color: 'var(--pf-t--global--text--color--regular)',
        lineHeight: '20px',
        whiteSpace: 'nowrap',
      }}>
        {label}
      </span>
    </span>
  );
}

function formatDiff(diff: number): string {
  const sign = diff > 0 ? '+' : '';
  return `${sign}${diff % 1 === 0 ? String(diff) : diff.toFixed(2)}`;
}

function formatFixed(positions: number, val?: number): string {
  if (val) {
    if (Number.isInteger(val)) {
      return val.toString();
    } else {
      return val.toFixed(positions);
    }
  }
  return '';
}

function DeltaCell({
  pctDiff,
  absDiff,
  absUnit,
}: {
  pctDiff?: number;
  absDiff?: number;
  absUnit?: string;
}) {
  if (absDiff === 0 && pctDiff === 0) {
    return <span>-</span>;
  }

  return (
    <span style={{ fontWeight: 500, whiteSpace: 'nowrap' }}>
      {absDiff !== undefined ? <>{formatDiff(absDiff)} {absUnit}</> : <></>}
      {absDiff !== undefined && pctDiff !== undefined ? <>&nbsp;/&nbsp;</> : <></>}
      {pctDiff !== undefined ? <>{formatDiff(pctDiff ?? '-')}%</> : <></>}
    </span>
  );
}

/** Label shown inside the bar: "123 MB – 45%" or just "45%". */
function buildBarLabel(pct: number, abs: number | null | undefined): string {
  const pctStr = `${pct.toFixed(1)}%`;
  if (abs == null) return pctStr;
  const absMB = abs % 1 === 0 ? String(abs) : abs.toFixed(1);
  return `${absMB} MB – ${pctStr}`;
}

function buildBrokerCapacity(id: string, brokerCapacity?: BrokerCapacity): string {
  if (brokerCapacity) {
    const brokerId = parseInt(id);

    const capacity: {
      cpu: string | null;
      inboundNetwork: string | null;
      outboundNetwork: string | null;
    } = brokerCapacity.overrides?.find(o => o.brokers?.includes(brokerId))
      ?? brokerCapacity;

    const elements = [
      (capacity.cpu ? 'CPU: ' + capacity.cpu : null),
      (capacity.inboundNetwork ? capacity.inboundNetwork + ' in' : null),
      (capacity.outboundNetwork ? capacity.outboundNetwork + ' out' : null)
    ];

    return elements.filter(s => s != null).join(", ");
  }

  return '-';
}

const DEFAULT_PAGE_SIZE = 20;

export function BrokerImpactTable({
  brokerCapacity,
  brokerImpact
}: BrokerImpactTableProps) {
  const { t } = useTranslation();

  const [nameFilter, setNameFilter] = useState('');
  const [selectedBrokers, setSelectedBrokers] = useState<string[]>([]);
  const [isBrokerSelectOpen, setIsBrokerSelectOpen] = useState(false);
  const [onlyDeltas, setOnlyDeltas] = useState(false);

  // Sort state
  const [sortKey, setSortKey] = useState<SortKey>('brokerId');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  // Pagination state
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(DEFAULT_PAGE_SIZE);

  // Only include groups whose pctKey is actually present in the data
  const activeGroups = useMemo(() => {
    if (!brokerImpact) return [];
    return COLUMN_GROUPS.filter((g) =>
      Object.values(brokerImpact).some((m) => (g.absKey ?? '' in m) || (g.pctKey ?? '' in m)),
    );
  }, [brokerImpact]);

  // Flat rows sorted by current sort state
  const allRows = useMemo((): BrokerRow[] => {
    if (!brokerImpact) return [];
    return Object.entries(brokerImpact)
      .map(([brokerId, metrics]) => ({ brokerId, metrics }))
      .sort((a, b) => {
        // eslint-disable-next-line no-useless-assignment
        let result = 0;

        if (sortKey === 'brokerId') {
          const aNum = parseInt(a.brokerId, 10);
          const bNum = parseInt(b.brokerId, 10);
          result = !isNaN(aNum) && !isNaN(bNum) ? aNum - bNum : a.brokerId.localeCompare(b.brokerId);
        } else {
          // sortKey is "<pctKey>-before", "<pctKey>-after", or "<pctKey>-delta"
          const lastDash = sortKey.lastIndexOf('-');
          const metricKey = sortKey.slice(0, lastDash) as string;
          const slot = sortKey.slice(lastDash + 1) as 'before' | 'after' | 'delta';
          const aVal = slot === 'delta' ? (a.metrics[metricKey]?.diff ?? 0) : (a.metrics[metricKey]?.[slot === 'before' ? 'before' : 'after'] ?? 0);
          const bVal = slot === 'delta' ? (b.metrics[metricKey]?.diff ?? 0) : (b.metrics[metricKey]?.[slot === 'before' ? 'before' : 'after'] ?? 0);
          result = (aVal as number) - (bVal as number);
        }

        return sortDirection === 'asc' ? result : -result;
      });
  }, [brokerImpact, sortKey, sortDirection]);

  const brokerIds = useMemo(() => allRows.map((r) => r.brokerId), [allRows]);

  const filteredRows = useMemo(() => {
    return allRows.filter((row) => {
      if (nameFilter && !row.brokerId.toLowerCase().includes(nameFilter.toLowerCase())) {
        return false;
      }
      if (selectedBrokers.length > 0 && !selectedBrokers.includes(row.brokerId)) {
        return false;
      }
      if (onlyDeltas) {
        const hasAnyDelta = activeGroups.some((g) => {
          const impact = row.metrics[g.absKey ?? ''] ?? row.metrics[g.pctKey ?? ''];
          return impact?.diff != null && impact.diff !== 0;
        });
        if (!hasAnyDelta) return false;
      }
      return true;
    });
  }, [allRows, nameFilter, selectedBrokers, onlyDeltas, activeGroups]);

  const pagedRows = useMemo(() => {
    const start = (page - 1) * perPage;
    return filteredRows.slice(start, start + perPage);
  }, [filteredRows, page, perPage]);

  const toggleBroker = (brokerId: string) => {
    setSelectedBrokers((prev) =>
      prev.includes(brokerId) ? prev.filter((b) => b !== brokerId) : [...prev, brokerId],
    );
  };

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDirection('asc');
    }
    setPage(1);
  };

  const getSortParams = (key: SortKey): ThProps['sort'] => ({
    sortBy: {
      index: 0,
      direction: sortKey === key ? sortDirection : undefined,
    },
    onSort: () => handleSort(key),
    columnIndex: 0,
  });

  if (!brokerImpact) {
    return (
      <EmptyState>
        <EmptyStateBody>{t('rebalancing.brokerImpact.noData')}</EmptyStateBody>
      </EmptyState>
    );
  }

  const brokerFilterLabels = selectedBrokers.map((b) => t('rebalancing.broker', { b }));

  const colCount = 1 + activeGroups.length * (onlyDeltas ? 1 : 3);

  return (
    <>
      <Toolbar clearAllFilters={() => { setNameFilter(''); setSelectedBrokers([]); setPage(1); }}>
        <ToolbarContent>
          <ToolbarGroup variant="filter-group" style={{ alignItems: 'center' }}>
            <ToolbarItem>
              <SearchInput
                placeholder={t('rebalancing.brokerImpact.findBroker')}
                value={nameFilter}
                onChange={(_e, val) => { setNameFilter(val); setPage(1); }}
                onClear={() => { setNameFilter(''); setPage(1); }}
              />
            </ToolbarItem>
            <ToolbarFilter
              labels={brokerFilterLabels}
              deleteLabel={(_category, chip) => {
                const brokerId = brokerIds.find(
                  (b) => t('rebalancing.broker', { b }) === chip,
                );
                if (brokerId) toggleBroker(brokerId);
              }}
              deleteLabelGroup={() => { setSelectedBrokers([]); setPage(1); }}
              categoryName={t('rebalancing.brokerImpact.brokers')}
            >
              <Select
                isOpen={isBrokerSelectOpen}
                onOpenChange={setIsBrokerSelectOpen}
                onSelect={(_e, val) => { toggleBroker(String(val)); setPage(1); }}
                selected={selectedBrokers}
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    onClick={() => setIsBrokerSelectOpen((o) => !o)}
                    isExpanded={isBrokerSelectOpen}
                  >
                    {t('rebalancing.brokerImpact.selectBrokers')}
                  </MenuToggle>
                )}
              >
                <SelectList>
                  {brokerIds.map((brokerId) => (
                    <SelectOption
                      key={brokerId}
                      value={brokerId}
                      hasCheckbox
                      isSelected={selectedBrokers.includes(brokerId)}
                    >
                      {t('rebalancing.broker', { b: brokerId })}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
            </ToolbarFilter>
          </ToolbarGroup>
          <ToolbarItem style={{ alignSelf: 'center' }}>
            <Switch
              id="only-deltas"
              label={t('rebalancing.brokerImpact.onlyShowDeltas')}
              isChecked={onlyDeltas}
              onChange={(_e, checked) => { setOnlyDeltas(checked); setPage(1); }}
            />
          </ToolbarItem>
          {(nameFilter || selectedBrokers.length > 0) && (
            <ToolbarItem>
              <Button
                variant="link"
                onClick={() => { setNameFilter(''); setSelectedBrokers([]); setPage(1); }}
              >
                {t('common.clearAllFilters')}
              </Button>
            </ToolbarItem>
          )}
          <ToolbarItem variant="pagination" align={{ default: 'alignEnd' }}>
            <Pagination
              itemCount={filteredRows.length}
              perPage={perPage}
              page={page}
              onSetPage={(_, newPage) => setPage(newPage)}
              onPerPageSelect={(_, newPerPage) => { setPerPage(newPerPage); setPage(1); }}
              variant="top"
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>

      <InnerScrollContainer>
        <Table aria-label={t('rebalancing.brokerImpact.tableLabel')} variant="compact">
          <Thead>
            <Tr>
              <Th sort={getSortParams('brokerId')} modifier="nowrap" isStickyColumn hasRightBorder>
                {t('rebalancing.brokerImpact.broker')}
              </Th>
              {activeGroups.map((g) => (
                <>
                  {!onlyDeltas && (
                    <Th key={`${g.pctKey}-before`} modifier="nowrap" sort={getSortParams(`${g.absKey ?? g.pctKey}-before`)}>
                      {g.label} {t('rebalancing.brokerImpact.before')}
                    </Th>
                  )}
                  {!onlyDeltas && (
                    <Th key={`${g.pctKey}-after`} modifier="nowrap" sort={getSortParams(`${g.absKey ?? g.pctKey}-after`)}>
                      {g.label} {t('rebalancing.brokerImpact.after')}
                    </Th>
                  )}
                  <Th key={`${g.pctKey}-delta`} modifier="nowrap" sort={getSortParams(`${g.absKey ?? g.pctKey}-delta`)}>
                    {g.label} Δ
                  </Th>
                </>
              ))}
              <Th modifier="nowrap">
                {t('rebalancing.brokerImpact.brokerCapacity')}
              </Th>
            </Tr>
          </Thead>
          <Tbody>
            {pagedRows.length === 0 ? (
              <Tr>
                <Td colSpan={colCount}>
                  <EmptyState>
                    <EmptyStateBody>{t('rebalancing.brokerImpact.noResults')}</EmptyStateBody>
                  </EmptyState>
                </Td>
              </Tr>
            ) : (
              pagedRows.map((row) => (
                <Tr key={row.brokerId}>
                  <Td dataLabel={t('rebalancing.brokerImpact.broker')} isStickyColumn hasRightBorder>
                    {t('rebalancing.broker', { b: row.brokerId })}
                  </Td>
                  {activeGroups.map((g) => {
                    const pctImpact = g.pctKey ? row.metrics[g.pctKey] : undefined;
                    const absImpact = g.absKey ? row.metrics[g.absKey] : undefined;
                    return (
                      <>
                        {!onlyDeltas && (
                          <Td key={`${row.brokerId}-${g.pctKey}-before`} dataLabel={`${g.label} ${t('rebalancing.brokerImpact.before')}`}>
                            {pctImpact
                              ? <BarCell
                                pct={pctImpact?.before}
                                label={buildBarLabel(pctImpact?.before ?? 0, absImpact?.before)}
                              />
                              : <>{formatFixed(2, absImpact?.before)} {g?.absUnit}</>}
                          </Td>
                        )}
                        {!onlyDeltas && (
                          <Td key={`${row.brokerId}-${g.pctKey}-after`} dataLabel={`${g.label} ${t('rebalancing.brokerImpact.after')}`}>
                            {pctImpact
                              ? <BarCell
                                pct={pctImpact?.after}
                                label={buildBarLabel(pctImpact?.after ?? 0, absImpact?.after)}
                              />
                              : <>{formatFixed(2, absImpact?.after)} {g?.absUnit}</>}
                          </Td>
                        )}
                        <Td key={`${row.brokerId}-${g.pctKey}-delta`} dataLabel={`${g.label} Δ`}>
                          <DeltaCell
                            pctDiff={pctImpact?.diff}
                            absDiff={absImpact?.diff}
                            absUnit={g?.absUnit}
                          />
                        </Td>
                      </>
                    );
                  })}
                  <Td dataLabel={t('rebalancing.brokerImpact.brokerCapacity')} modifier='nowrap'>
                    {buildBrokerCapacity(row.brokerId, brokerCapacity)}
                  </Td>
                </Tr>
              ))
            )}
          </Tbody>
        </Table>
      </InnerScrollContainer>

      <Pagination
        itemCount={filteredRows.length}
        perPage={perPage}
        page={page}
        onSetPage={(_, newPage) => setPage(newPage)}
        onPerPageSelect={(_, newPerPage) => { setPerPage(newPerPage); setPage(1); }}
        variant="bottom"
      />
    </>
  );
}
