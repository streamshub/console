/**
 * Advanced Search Component for Messages
 * Provides filtering options for Kafka messages
 */

import { useState, useCallback, useEffect, useTransition } from 'react';
import { useTranslation } from 'react-i18next';
import {
  SearchInput,
  Button,
  Form,
  FormGroup,
  TextInput,
  Panel,
  PanelMain,
  PanelMainBody,
  ActionGroup,
  Grid,
  GridItem,
  FormSection,
  MenuToggle,
  Select,
  SelectOption,
  SelectList,
  Flex,
  FlexItem,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Divider,
} from '@patternfly/react-core';
import { SearchParams } from '@/api/types';
import { parseSearchInput } from './parseSearchInput';

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

type FromCategory = 'offset' | 'timestamp' | 'epoch' | 'latest';
type RetrieveCategory = 'limit' | 'continuously';

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
  const DEFAULT_LIMIT = 50;
  
  const [isExpanded, setIsExpanded] = useState(false);
  const [query, setQuery] = useState(filterQuery || '');
  const [where, setWhere] = useState<'key' | 'headers' | 'value' | undefined>(filterWhere);
  const [partition, setPartition] = useState<number | undefined>(filterPartition);
  
  // From group state
  const [fromCategory, setFromCategory] = useState<FromCategory>(
    filterOffset !== undefined ? 'offset' :
    filterTimestamp !== undefined ? 'timestamp' :
    filterEpoch !== undefined ? 'epoch' : 'latest'
  );
  const [fromOffset, setFromOffset] = useState<number | undefined>(filterOffset);
  const [fromTimestamp, setFromTimestamp] = useState<string | undefined>(filterTimestamp);
  const [fromEpoch, setFromEpoch] = useState<number | undefined>(filterEpoch);
  const [isFromOpen, setIsFromOpen] = useState(false);
  const [_, startTransition] = useTransition();
  
  // Retrieve group state
  const [retrieveCategory, setRetrieveCategory] = useState<RetrieveCategory>(
    filterLimit === 'continuously' ? 'continuously' : 'limit'
  );
  const [limit, setLimit] = useState<number>(
    typeof filterLimit === 'number' ? filterLimit : DEFAULT_LIMIT
  );
  const [isRetrieveOpen, setIsRetrieveOpen] = useState(false);
  const [isLimitOpen, setIsLimitOpen] = useState(false);
  
  // Where selector state
  const [isWhereOpen, setIsWhereOpen] = useState(false);
  
  // Partition selector state
  const [isPartitionOpen, setIsPartitionOpen] = useState(false);

  const getParameters = useCallback((): SearchParams => {
    const from: SearchParams['from'] = (() => {
      if (fromCategory === 'offset' && fromOffset !== undefined) {
        return { type: 'offset', value: fromOffset };
      } else if (fromCategory === 'epoch' && fromEpoch !== undefined) {
        return { type: 'epoch', value: fromEpoch };
      } else if (fromCategory === 'timestamp' && fromTimestamp) {
        return { type: 'timestamp', value: fromTimestamp };
      }
      return { type: 'latest' };
    })();

    return {
      query: query ? { value: query, where: where ?? 'everywhere' } : undefined,
      partition,
      from,
      limit: retrieveCategory === 'continuously' ? 'continuously' : limit,
    };
  }, [query, where, partition, fromCategory, fromOffset, fromEpoch, fromTimestamp, retrieveCategory, limit]);

  const getSearchInputValue = useCallback(() => {
    const parameters = getParameters();
    const { query, from, limit, partition } = parameters;
    return (() => {
      let composed: string[] = [];
      if (from !== undefined) {
        if ("value" in from) {
          composed.push(`messages=${from.type}:${from.value}`);
        } else {
          composed.push(`messages=latest`);
        }
      }
      if (limit !== undefined) {
        composed.push(`retrieve=${limit}`);
      }
      if (partition !== undefined) {
        composed.push(`partition=${partition}`);
      }
      if (query !== undefined) {
        composed.push(query.value);
        if (query.where !== "everywhere") {
          composed.push(`where=${query.where}`);
        }
      }
      return composed.join(" ");
    })();
  }, [getParameters]);

  const [searchInputValue, setSearchInputValue] = useState(
    getSearchInputValue(),
  );

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    onSearch(getParameters());
    setIsExpanded(false);
  };

  const handleReset = () => {
    setQuery('');
    setWhere(undefined);
    setPartition(undefined);
    setFromCategory('latest');
    setFromOffset(undefined);
    setFromTimestamp(undefined);
    setFromEpoch(undefined);
    setRetrieveCategory('limit');
    setLimit(DEFAULT_LIMIT);
    
    onSearch({
      query: undefined,
      partition: undefined,
      from: { type: 'latest' },
      limit: DEFAULT_LIMIT,
    });
    
    setIsExpanded(false);
  };

  useEffect(() => {
    setSearchInputValue(getSearchInputValue());
  }, [getSearchInputValue]);

  return (
    <div style={{ position: 'relative', width: '100%' }}>
      <SearchInput
        placeholder={t('common.search')}
        value={searchInputValue}
        onChange={(_, v) => setSearchInputValue(v)}
        onSearch={(e) => {
          e.preventDefault();
          const sp = parseSearchInput({
            value: searchInputValue,
          });
          startTransition(() => {
            onSearch(sp);
          });
        }}
        onClear={() => handleReset()}
        onToggleAdvancedSearch={() => setIsExpanded(!isExpanded)}
        isAdvancedSearchOpen={isExpanded}
      />
      {isExpanded && <Panel
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
                      <FormHelperText>
                        <HelperText>
                          <HelperTextItem>
                            {t('topics.messages.advancedSearch.queryHelper')}
                          </HelperTextItem>
                        </HelperText>
                      </FormHelperText>
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.where')}>
                      <Select
                        isOpen={isWhereOpen}
                        onOpenChange={setIsWhereOpen}
                        onSelect={(_, value) => {
                          setWhere(value === 'everywhere' ? undefined : value as 'key' | 'headers' | 'value');
                          setIsWhereOpen(false);
                        }}
                        toggle={(toggleRef) => (
                          <MenuToggle
                            ref={toggleRef}
                            onClick={() => setIsWhereOpen(!isWhereOpen)}
                            isExpanded={isWhereOpen}
                            style={{ width: '100%' }}
                          >
                            {where === 'value' ? t('topics.messages.advancedSearch.whereValue') :
                             where === 'key' ? t('topics.messages.advancedSearch.whereKey') :
                             where === 'headers' ? t('topics.messages.advancedSearch.whereHeaders') :
                             'Anywhere'}
                          </MenuToggle>
                        )}
                      >
                        <SelectList>
                          <SelectOption value="everywhere" isSelected={where === undefined}>
                            Anywhere
                          </SelectOption>
                          <SelectOption value="key" isSelected={where === 'key'}>
                            {t('topics.messages.advancedSearch.whereKey')}
                          </SelectOption>
                          <SelectOption value="headers" isSelected={where === 'headers'}>
                            {t('topics.messages.advancedSearch.whereHeaders')}
                          </SelectOption>
                          <SelectOption value="value" isSelected={where === 'value'}>
                            {t('topics.messages.advancedSearch.whereValue')}
                          </SelectOption>
                        </SelectList>
                      </Select>
                    </FormGroup>
                  </GridItem>
                </Grid>
              </FormSection>

              <FormSection title="Parameters">
                <Grid hasGutter>
                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.messages')}>
                      <Flex direction={{ default: 'column' }}>
                        <FlexItem>
                          <Select
                            isOpen={isFromOpen}
                            onOpenChange={setIsFromOpen}
                            onSelect={(_, value) => {
                              const newCategory = value as FromCategory;
                              setFromCategory(newCategory);
                              if (newCategory === 'latest') {
                                setFromOffset(undefined);
                                setFromTimestamp(undefined);
                                setFromEpoch(undefined);
                              }
                              setIsFromOpen(false);
                            }}
                            toggle={(toggleRef) => (
                              <MenuToggle
                                ref={toggleRef}
                                onClick={() => setIsFromOpen(!isFromOpen)}
                                isExpanded={isFromOpen}
                                style={{ width: '100%' }}
                              >
                                {fromCategory === 'offset' ? t('topics.messages.filter.offset') :
                                 fromCategory === 'timestamp' ? t('topics.messages.filter.timestamp') :
                                 fromCategory === 'epoch' ? t('topics.messages.filter.epoch') :
                                 t('topics.messages.filter.latest')}
                              </MenuToggle>
                            )}
                          >
                            <SelectList>
                              <SelectOption value="offset">{t('topics.messages.filter.offset')}</SelectOption>
                              <SelectOption value="timestamp">{t('topics.messages.filter.timestamp')}</SelectOption>
                              <SelectOption value="epoch">{t('topics.messages.filter.epoch')}</SelectOption>
                              <Divider component="li" />
                              <SelectOption value="latest">{t('topics.messages.filter.latest')}</SelectOption>
                            </SelectList>
                          </Select>
                        </FlexItem>
                        {fromCategory === 'offset' && (
                          <FlexItem>
                            <TextInput
                              type="number"
                              value={fromOffset?.toString() || ''}
                              onChange={(_, value) => setFromOffset(value ? parseInt(value) : undefined)}
                              placeholder={t('topics.messages.filter.offsetPlaceholder')}
                              aria-label={t('topics.messages.filter.offsetAriaLabel')}
                            />
                          </FlexItem>
                        )}
                        {fromCategory === 'timestamp' && (
                          <FlexItem>
                            <TextInput
                              type="datetime-local"
                              value={fromTimestamp || ''}
                              onChange={(_, value) => setFromTimestamp(value || undefined)}
                              aria-label={t('topics.messages.filter.timestampAriaLabel')}
                            />
                          </FlexItem>
                        )}
                        {fromCategory === 'epoch' && (
                          <FlexItem>
                            <TextInput
                              type="number"
                              value={fromEpoch?.toString() || ''}
                              onChange={(_, value) => setFromEpoch(value ? parseInt(value) : undefined)}
                              placeholder={t('topics.messages.filter.epochPlaceholder')}
                              aria-label={t('topics.messages.filter.epochAriaLabel')}
                            />
                          </FlexItem>
                        )}
                      </Flex>
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.retrieve')}>
                      <Flex direction={{ default: 'column' }}>
                        <FlexItem>
                          <Select
                            isOpen={isRetrieveOpen}
                            onOpenChange={setIsRetrieveOpen}
                            onSelect={(_, value) => {
                              const newCategory = value as RetrieveCategory;
                              setRetrieveCategory(newCategory);
                              setIsRetrieveOpen(false);
                            }}
                            toggle={(toggleRef) => (
                              <MenuToggle
                                ref={toggleRef}
                                onClick={() => setIsRetrieveOpen(!isRetrieveOpen)}
                                isExpanded={isRetrieveOpen}
                                style={{ width: '100%' }}
                              >
                                {retrieveCategory === 'limit' ? 'Number of messages' : t('topics.messages.advancedSearch.continuously')}
                              </MenuToggle>
                            )}
                          >
                            <SelectList>
                              <SelectOption value="limit">Number of messages</SelectOption>
                              <SelectOption value="continuously">{t('topics.messages.advancedSearch.continuously')}</SelectOption>
                            </SelectList>
                          </Select>
                        </FlexItem>
                        {retrieveCategory === 'limit' && (
                          <FlexItem>
                            <Select
                              isOpen={isLimitOpen}
                              onOpenChange={setIsLimitOpen}
                              selected={limit}
                              onSelect={(_, value) => {
                                setLimit(value as number);
                                setIsLimitOpen(false);
                              }}
                              toggle={(toggleRef) => (
                                <MenuToggle
                                  ref={toggleRef}
                                  onClick={() => setIsLimitOpen(!isLimitOpen)}
                                  isExpanded={isLimitOpen}
                                  style={{ width: '100%' }}
                                >
                                  {limit}
                                </MenuToggle>
                              )}
                            >
                              <SelectList>
                                {[5, 10, 25, 50, 75, 100].map((value) => (
                                  <SelectOption key={value} value={value}>
                                    {value}
                                  </SelectOption>
                                ))}
                              </SelectList>
                            </Select>
                          </FlexItem>
                        )}
                        {retrieveCategory === 'continuously' && (
                          <FlexItem>
                            <FormHelperText>
                              <HelperText>
                                <HelperTextItem>
                                  The screen displays only the most recent 100 messages, with older messages rotating out.
                                </HelperTextItem>
                              </HelperText>
                            </FormHelperText>
                          </FlexItem>
                        )}
                      </Flex>
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={t('topics.messages.advancedSearch.inPartition')}>
                      <Select
                        isOpen={isPartitionOpen}
                        onOpenChange={setIsPartitionOpen}
                        isScrollable
                        onSelect={(_, value) => {
                          setPartition(value === -1 ? undefined : value as number);
                          setIsPartitionOpen(false);
                        }}
                        toggle={(toggleRef) => (
                          <MenuToggle
                            ref={toggleRef}
                            onClick={() => setIsPartitionOpen(!isPartitionOpen)}
                            isExpanded={isPartitionOpen}
                            style={{ width: '100%' }}
                          >
                            {partition !== undefined
                              ? t('topics.messages.partitionOption', { value: partition })
                              : t('topics.messages.partitionPlaceholder')}
                          </MenuToggle>
                        )}
                      >
                        <SelectList>
                          <SelectOption value={-1} isSelected={partition === undefined}>
                            {t('topics.messages.partitionPlaceholder')}
                          </SelectOption>
                          {partitions > 0 && Array.from({ length: partitions }, (_, i) => (
                            <SelectOption key={i} value={i} isSelected={partition === i}>
                              {i}
                            </SelectOption>
                          ))}
                        </SelectList>
                      </Select>
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
      </Panel>}
    </div>
  );
}