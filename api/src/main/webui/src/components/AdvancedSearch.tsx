/**
 * Advanced Search Component for Messages
 * Provides filtering options for Kafka messages
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  SearchInput,
  Button,
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption,
  Panel,
  PanelMain,
  PanelMainBody,
  ActionGroup,
  Grid,
  GridItem,
  FormSection,
} from '@patternfly/react-core';
import { SearchParams } from '../api/types';

interface AdvancedSearchProps {
  partitions: number;
  filterQuery?: string;
  filterWhere?: 'key' | 'headers' | 'value';
  filterPartition?: number;
  filterOffset?: number;
  filterTimestamp?: string;
  filterEpoch?: number;
  filterLimit?: number | 'continuously';
  onSearch: (params: SearchParams) => void;
}

export function AdvancedSearch({
  partitions,
  filterQuery,
  filterWhere,
  filterPartition,
  filterOffset,
  filterTimestamp,
  filterEpoch,
  filterLimit,
  onSearch,
}: AdvancedSearchProps) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);
  const [query, setQuery] = useState(filterQuery || '');
  const [where, setWhere] = useState<'key' | 'headers' | 'value' | 'everywhere'>(
    filterWhere || 'everywhere'
  );
  const [partition, setPartition] = useState<string>(
    filterPartition !== undefined ? String(filterPartition) : 'all'
  );
  const [fromType, setFromType] = useState<'latest' | 'offset' | 'timestamp' | 'epoch'>(
    filterOffset !== undefined ? 'offset' :
    filterTimestamp !== undefined ? 'timestamp' :
    filterEpoch !== undefined ? 'epoch' : 'latest'
  );
  const [offsetValue, setOffsetValue] = useState(filterOffset?.toString() || '');
  const [timestampValue, setTimestampValue] = useState(filterTimestamp || '');
  const [epochValue, setEpochValue] = useState(filterEpoch?.toString() || '');
  const [limit, setLimit] = useState<string>(
    filterLimit === 'continuously' ? 'continuously' : String(filterLimit || 50)
  );

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    
    const from: SearchParams['from'] = (() => {
      if (fromType === 'offset' && offsetValue) {
        return { type: 'offset', value: parseInt(offsetValue) };
      } else if (fromType === 'timestamp' && timestampValue) {
        return { type: 'timestamp', value: timestampValue };
      } else if (fromType === 'epoch' && epochValue) {
        return { type: 'epoch', value: parseInt(epochValue) };
      }
      return { type: 'latest' };
    })();

    const limitValue = limit === 'continuously' ? 'continuously' as const : parseInt(limit);

    onSearch({
      query: query ? { value: query, where } : undefined,
      partition: partition === 'all' ? undefined : parseInt(partition),
      from,
      limit: limitValue,
    });
    
    setIsExpanded(false);
  };

  const handleReset = () => {
    setQuery('');
    setWhere('everywhere');
    setPartition('all');
    setFromType('latest');
    setOffsetValue('');
    setTimestampValue('');
    setEpochValue('');
    setLimit('50');
    
    onSearch({
      query: undefined,
      partition: undefined,
      from: { type: 'latest' },
      limit: 50,
    });
    
    setIsExpanded(false);
  };

  const searchInput = (
    <SearchInput
      placeholder={t('common.search')}
      value={query}
      onChange={(_, value) => setQuery(value)}
      onSearch={handleSubmit}
      onClear={() => setQuery('')}
      onToggleAdvancedSearch={() => setIsExpanded(!isExpanded)}
      isAdvancedSearchOpen={isExpanded}
    />
  );

  if (!isExpanded) {
    return searchInput;
  }

  return (
    <div style={{ position: 'relative', width: '100%' }}>
      {searchInput}
      <Panel
        variant="raised"
        style={{
          position: 'absolute',
          top: '100%',
          left: 0,
          right: 0,
          zIndex: 1000,
          marginTop: '0.5rem',
        }}
      >
        <PanelMain>
          <PanelMainBody>
            <Form onSubmit={handleSubmit}>
              <FormSection title={t('common.filter')}>
                <Grid hasGutter>
                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.hasTheWords')}>
                      <TextInput
                        type="text"
                        value={query}
                        onChange={(_, value) => setQuery(value)}
                        placeholder={t('topics.messages.advancedSearch.queryHelper')}
                      />
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.where')}>
                      <FormSelect
                        value={where}
                        onChange={(_, value) => setWhere(value as typeof where)}
                      >
                        <FormSelectOption value="everywhere" label={t('topics.messages.advancedSearch.whereEverywhere')} />
                        <FormSelectOption value="key" label={t('topics.messages.advancedSearch.whereKey')} />
                        <FormSelectOption value="value" label={t('topics.messages.advancedSearch.whereValue')} />
                        <FormSelectOption value="headers" label={t('topics.messages.advancedSearch.whereHeaders')} />
                      </FormSelect>
                    </FormGroup>
                  </GridItem>
                </Grid>
              </FormSection>

              <FormSection title="Parameters">
                <Grid hasGutter>
                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.messages')}>
                      <FormSelect
                        value={fromType}
                        onChange={(_, value) => setFromType(value as typeof fromType)}
                      >
                        <FormSelectOption value="latest" label={t('topics.messages.filter.latest')} />
                        <FormSelectOption value="offset" label={t('topics.messages.filter.offset')} />
                        <FormSelectOption value="timestamp" label={t('topics.messages.filter.timestamp')} />
                        <FormSelectOption value="epoch" label={t('topics.messages.filter.epoch')} />
                      </FormSelect>
                      
                      {fromType === 'offset' && (
                        <TextInput
                          type="number"
                          value={offsetValue}
                          onChange={(_, value) => setOffsetValue(value)}
                          placeholder={t('topics.messages.filter.offsetPlaceholder')}
                          style={{ marginTop: '0.5rem' }}
                        />
                      )}
                      
                      {fromType === 'timestamp' && (
                        <TextInput
                          type="datetime-local"
                          value={timestampValue}
                          onChange={(_, value) => setTimestampValue(value)}
                          style={{ marginTop: '0.5rem' }}
                        />
                      )}
                      
                      {fromType === 'epoch' && (
                        <TextInput
                          type="number"
                          value={epochValue}
                          onChange={(_, value) => setEpochValue(value)}
                          placeholder={t('topics.messages.filter.epochPlaceholder')}
                          style={{ marginTop: '0.5rem' }}
                        />
                      )}
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.retrieve')}>
                      <TextInput
                        type="number"
                        value={limit === 'continuously' ? '' : limit}
                        onChange={(_, value) => setLimit(value || '50')}
                        placeholder="50"
                      />
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.inPartition')}>
                      <FormSelect
                        value={partition}
                        onChange={(_, value) => setPartition(value)}
                      >
                        <FormSelectOption value="all" label={t('topics.messages.partitionPlaceholder')} />
                        {Array.from({ length: partitions }, (_, i) => (
                          <FormSelectOption
                            key={i}
                            value={String(i)}
                            label={t('topics.messages.partitionOption', { value: i })}
                          />
                        ))}
                      </FormSelect>
                    </FormGroup>
                  </GridItem>
                </Grid>
              </FormSection>

              <ActionGroup>
                <Button variant="primary" type="submit">
                  {t('topics.messages.advancedSearch.search')}
                </Button>
                <Button variant="link" onClick={handleReset}>
                  {t('topics.messages.advancedSearch.reset')}
                </Button>
              </ActionGroup>
            </Form>
          </PanelMainBody>
        </PanelMain>
      </Panel>
    </div>
  );
}