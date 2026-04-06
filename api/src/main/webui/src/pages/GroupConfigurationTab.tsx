/**
 * Group Configuration Tab - Shows configuration settings for a consumer group
 */

import { useOutletContext } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState, useMemo, useCallback } from 'react';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Button,
  Label,
  LabelGroup,
  List,
  ListItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  SearchInput,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from '@patternfly/react-core';
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ThProps,
} from '@patternfly/react-table';
import { SearchIcon } from '@patternfly/react-icons';
import { Group, ConfigValue } from '../api/types';

interface GroupOutletContext {
  group: Group;
}

type SortableColumn = 'property' | 'value';

function formatConfigValue(property: ConfigValue): React.ReactNode {
  switch (property.type) {
    case 'INT':
    case 'LONG':
      return property.value ? parseInt(property.value, 10).toLocaleString() : '-';
    case 'STRING':
      if (property.source === 'STATIC_BROKER_CONFIG') {
        // STATIC_BROKER_CONFIG strings are actually lists in disguise
        return (
          <List isPlain isBordered>
            {property.value
              ?.split(',')
              .map((v, idx) => <ListItem key={idx}>{v || '-'}</ListItem>)}
          </List>
        );
      }
      return property.value || '-';
    case 'LIST':
      return (
        <List isPlain isBordered>
          {property.value
            ?.split(',')
            .map((v, idx) => <ListItem key={idx}>{v || '-'}</ListItem>)}
        </List>
      );
    default:
      if (property.sensitive) {
        return '******';
      }
      return property.value || '-';
  }
}

function NoResultsEmptyState({ onReset }: { onReset: () => void }) {
  const { t } = useTranslation();
  return (
    <EmptyState variant="lg" icon={SearchIcon}>
      <Title headingLevel="h4" size="lg">
        {t('topics.configuration.noResultsTitle')}
      </Title>
      <EmptyStateBody>{t('topics.configuration.noResultsBody')}</EmptyStateBody>
      <Button variant="link" onClick={onReset}>
        {t('topics.configuration.noResultsReset')}
      </Button>
    </EmptyState>
  );
}

export function GroupConfigurationTab() {
  const { t } = useTranslation();
  const { group } = useOutletContext<GroupOutletContext>();

  const [propertyFilter, setPropertyFilter] = useState('');
  const [selectedDataSources, setSelectedDataSources] = useState<string[]>([]);
  const [isDataSourceSelectOpen, setIsDataSourceSelectOpen] = useState(false);
  const [sortColumn, setSortColumn] = useState<SortableColumn>('property');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const allData = Object.entries(group.attributes.configs || {});

  // Derive available data sources from config values
  const dataSources = useMemo(() => {
    return Array.from(new Set(allData.map(([_, property]) => property.source)));
  }, [allData]);

  // Initialize selected data sources to all sources
  useMemo(() => {
    if (dataSources.length > 0 && selectedDataSources.length === 0) {
      setSelectedDataSources(dataSources);
    }
  }, [dataSources, selectedDataSources.length]);

  // Filter and sort data
  const filteredAndSortedData = useMemo(() => {
    let filtered = allData
      .filter(([name]) => (propertyFilter ? name.includes(propertyFilter) : true))
      .filter(([_, property]) =>
        selectedDataSources.length > 0 ? selectedDataSources.includes(property.source) : true
      );

    // Sort
    filtered = [...filtered].sort((a, b) => {
      let comparison = 0;
      if (sortColumn === 'property') {
        comparison = a[0].localeCompare(b[0]);
      } else {
        comparison = (a[1].value || '').localeCompare(b[1].value || '');
      }
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return filtered;
  }, [allData, propertyFilter, selectedDataSources, sortColumn, sortDirection]);

  const handleSort = (column: SortableColumn) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const getSortParams = (column: SortableColumn): ThProps['sort'] => ({
    sortBy: {
      index: 0,
      direction: sortColumn === column ? sortDirection : undefined,
    },
    onSort: () => handleSort(column),
    columnIndex: 0,
  });

  const handleReset = useCallback(() => {
    setPropertyFilter('');
    setSelectedDataSources(dataSources);
  }, [dataSources]);

  const handleDataSourceToggle = (source: string) => {
    setSelectedDataSources((prev) =>
      prev.includes(source) ? prev.filter((s) => s !== source) : [...prev, source]
    );
  };

  if (allData.length === 0) {
    return (
      <PageSection>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('topics.configuration.noConfiguration')}
          </Title>
          <EmptyStateBody>
            {t('topics.configuration.noConfigurationDescription')}
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  const isFiltered = propertyFilter !== '' || selectedDataSources.length !== dataSources.length;
  const showEmptyState = isFiltered && filteredAndSortedData.length === 0;

  return (
    <PageSection>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <SearchInput
              placeholder={t('topics.configuration.searchPlaceholder')}
              value={propertyFilter}
              onChange={(_, value) => setPropertyFilter(value)}
              onClear={() => setPropertyFilter('')}
              aria-label={t('topics.configuration.searchPlaceholder')}
            />
          </ToolbarItem>
          <ToolbarItem>
            <Select
              id="data-source-select"
              isOpen={isDataSourceSelectOpen}
              selected={selectedDataSources}
              onSelect={(_, selection) => handleDataSourceToggle(selection as string)}
              onOpenChange={(isOpen) => setIsDataSourceSelectOpen(isOpen)}
              toggle={(toggleRef) => (
                <MenuToggle
                  ref={toggleRef}
                  onClick={() => setIsDataSourceSelectOpen(!isDataSourceSelectOpen)}
                  isExpanded={isDataSourceSelectOpen}
                >
                  {t('topics.configuration.dataSource')} (
                  {selectedDataSources.length === dataSources.length
                    ? t('topics.configuration.all')
                    : selectedDataSources.length}
                  )
                </MenuToggle>
              )}
            >
              <SelectList>
                {dataSources.map((source) => (
                  <SelectOption
                    key={source}
                    value={source}
                    hasCheckbox
                    isSelected={selectedDataSources.includes(source)}
                  >
                    {source}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>
          </ToolbarItem>
          {isFiltered && (
            <ToolbarItem>
              <Button variant="link" onClick={handleReset}>
                {t('topics.configuration.clearFilters')}
              </Button>
            </ToolbarItem>
          )}
        </ToolbarContent>
      </Toolbar>

      {showEmptyState ? (
        <NoResultsEmptyState onReset={handleReset} />
      ) : (
        <Table aria-label="Group configuration table" variant="compact">
          <Thead>
            <Tr>
              <Th width={40} sort={getSortParams('property')}>
                {t('topics.configuration.property')}
              </Th>
              <Th sort={getSortParams('value')}>{t('topics.configuration.value')}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {filteredAndSortedData.map(([name, property]) => (
              <Tr key={name}>
                <Td dataLabel={t('topics.configuration.property')} style={{ verticalAlign: 'middle' }}>
                  <div>{name}</div>
                  <LabelGroup>
                    <Label isCompact color="teal">
                      source={property.source}
                    </Label>
                    {property.readOnly && (
                      <Label isCompact color="grey">
                        {t('topics.configuration.readOnly')}
                      </Label>
                    )}
                  </LabelGroup>
                </Td>
                <Td dataLabel={t('topics.configuration.value')} style={{ verticalAlign: 'middle' }}>
                  {formatConfigValue(property)}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </PageSection>
  );
}