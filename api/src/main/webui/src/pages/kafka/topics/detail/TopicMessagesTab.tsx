/**
 * Topic Messages Tab - Shows messages in a topic with advanced search and detail drawer
 */

import { useState, useCallback, useMemo } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Drawer,
  DrawerContent,
  DrawerContentBody,
  DrawerPanelContent,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  EmptyState,
  EmptyStateBody,
  Button,
  Spinner,
  Alert,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Tooltip,
} from '@patternfly/react-core';
import { ExclamationCircleIcon, ColumnsIcon } from '@patternfly/react-icons';
import { useMessages } from '@/api/hooks/useMessages';
import { useTopic } from '@/api/hooks/useTopics';
import { ApiError } from '@/api/client';
import { KafkaRecord, SearchParams } from '@/api/types';
import { MessagesTable } from '@/components/kafka/topics/MessagesTable';
import { MessageDetails } from '@/components/kafka/topics/MessageDetails';
import { AdvancedSearch } from '@/components/kafka/topics/AdvancedSearch';
import { Column, ColumnsModal } from '@/components/kafka/topics/ColumnsModal';

const defaultColumns: Column[] = [
  'offset-partition',
  'timestampUTC',
  'key',
  'headers',
  'value',
  'size',
];

export function TopicMessagesTab() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{ kafkaId: string; topicId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Parse search params
  const retrieveParam = searchParams.get('retrieve');
  const limit = retrieveParam === 'continuously'
    ? 'continuously' as const
    : parseInt(retrieveParam || '50');
  const partitionParam = searchParams.get('partition');
  const partition = partitionParam ? parseInt(partitionParam) : undefined;
  const query = searchParams.get('query') || undefined;
  const where = (searchParams.get('where') as 'key' | 'headers' | 'value') || undefined;
  const offsetParam = searchParams.get('offset');
  const offset = offsetParam ? parseInt(offsetParam) : undefined;
  const timestamp = searchParams.get('timestamp') || undefined;
  const epochParam = searchParams.get('epoch');
  const epoch = epochParam ? parseInt(epochParam) : undefined;
  const [showColumnsModal, setShowColumnsModal] = useState(false);

  // Fetch topic info to get partition count
  const { data: topic, isLoading: isLoadingTopic } = useTopic(
    kafkaId!,
    topicId!
  );

  const topicData = topic?.data;
  const partitions = topicData?.attributes?.numPartitions || 0;

  // Fetch messages
  const {
    data: messages = [],
    isLoading: isLoadingMessages,
    error: messagesError,
    refetch,
  } = useMessages(
    {
      kafkaId: kafkaId!,
      topicId: topicId!,
      pageSize: limit === 'continuously' ? 50 : limit,
      partition,
      query,
      where,
      offset,
      timestamp,
      epoch,
    },
    {
      enabled: !!kafkaId && !!topicId,
      refetchInterval: limit === 'continuously' ? 1000 : false,
    }
  );

  const selectedMessageId = useMemo(() => searchParams.get('selected'), [searchParams]);
  // Derive selected message directly from URL + messages — no separate state needed.
  const selectedMessage = useMemo(() => {
    if (!selectedMessageId) return undefined;
    const [partStr, offsetStr] = selectedMessageId.split(':');
    const part = parseInt(partStr);
    const off = parseInt(offsetStr);
    return messages.find(
      m => m.attributes.partition === part && m.attributes.offset === off
    );
  }, [selectedMessageId, messages]);

  const handleSearch = useCallback((params: SearchParams) => {
    const newParams = new URLSearchParams();
    
    if (params.query?.value) {
      newParams.set('query', params.query.value);
      if (params.query.where !== 'everywhere') {
        newParams.set('where', params.query.where);
      }
    }
    
    if (params.partition !== undefined) {
      newParams.set('partition', String(params.partition));
    }
    
    if (params.from.type !== 'latest') {
      if (params.from.type === 'offset') {
        newParams.set('offset', String(params.from.value));
      } else if (params.from.type === 'timestamp') {
        newParams.set('timestamp', params.from.value);
      } else if (params.from.type === 'epoch') {
        newParams.set('epoch', String(params.from.value));
      }
    }
    
    if (params.limit !== 50) {
      newParams.set('retrieve', String(params.limit));
    }
    
    setSearchParams(newParams, { replace: true });
    // Always refetch explicitly — if the params haven't changed the query key
    // is identical and TanStack Query won't issue a new request otherwise.
    refetch();
  }, [setSearchParams, refetch]);

  const handleSelectMessage = useCallback((message: KafkaRecord) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('selected', `${message.attributes.partition}:${message.attributes.offset}`);
    setSearchParams(newParams, { replace: true });
  }, [searchParams, setSearchParams]);

  const handleDeselectMessage = useCallback(() => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete('selected');
    setSearchParams(newParams, { replace: true });
  }, [searchParams, setSearchParams]);

  const handleReset = useCallback(() => {
    setSearchParams(new URLSearchParams());
  }, [setSearchParams]);

  // Load saved column preferences from localStorage
  const persistedColumns = useMemo(
    () => {
      const columnsJson = localStorage.getItem('message-browser-columns');
      if (columnsJson) {
        try {
          const parsed = JSON.parse(columnsJson);
          if (Array.isArray(parsed)) {
            return parsed;
          }
        } catch {
          // Ignore parse errors
        }
      }
    },
    []
  );

  const [chosenColumns, setChosenColumns] = useState<Column[]>(
    persistedColumns ?? defaultColumns
  );

  const handleColumnsConfirm = (columns: Column[]) => {
    setChosenColumns(columns);
    localStorage.setItem('message-browser-columns', JSON.stringify(columns));
    setShowColumnsModal(false);
  };

  // Loading state
  if (isLoadingTopic || (isLoadingMessages && messages.length === 0)) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  const hasFilters = partition !== undefined || offset !== undefined ||
                     timestamp !== undefined || epoch !== undefined || query !== undefined;

  return (
    <>
    <Drawer isExpanded={!!selectedMessage}>
      <DrawerContent
        panelContent={
          selectedMessage && (
            <DrawerPanelContent isResizable minSize="400px">
              <DrawerHead>
                <span>{t('topics.messages.message')}</span>
                <DrawerActions>
                  <DrawerCloseButton onClick={handleDeselectMessage} />
                </DrawerActions>
              </DrawerHead>
              <MessageDetails message={selectedMessage} />
            </DrawerPanelContent>
          )
        }
      >
        <DrawerContentBody>
          <PageSection isFilled>
            {limit === 'continuously' && (
              <Alert
                variant="info"
                isInline
                title={t('topics.messages.continuousMode.title')}
              />
            )}

            <Toolbar>
              <ToolbarContent>
                <ToolbarItem style={{ flex: 1, maxWidth: '700px' }}>
                  <AdvancedSearch
                    partitions={partitions}
                    filterQuery={query}
                    filterWhere={where}
                    filterPartition={partition}
                    filterOffset={offset}
                    filterTimestamp={timestamp}
                    filterEpoch={epoch}
                    filterLimit={limit}
                    onSearch={handleSearch}
                  />
                </ToolbarItem>
                <ToolbarItem>
                  <Tooltip content={t('topics.messages.manage_columns')}>
                    <Button
                      variant="plain"
                      icon={<ColumnsIcon />}
                      onClick={() => setShowColumnsModal(true)}
                      aria-label={t('topics.messages.manage_columns')}
                    />
                  </Tooltip>
                </ToolbarItem>
              </ToolbarContent>
            </Toolbar>

            {messagesError ? (
              <EmptyState>
                <ExclamationCircleIcon />
                <Title headingLevel="h2" size="lg">
                  {messagesError instanceof ApiError && messagesError.errors?.[0]?.title
                    ? messagesError.errors[0].title
                    : t('topics.messages.noDataTitle')}
                </Title>
                <EmptyStateBody>
                  {messagesError instanceof ApiError && messagesError.errors?.[0]?.detail
                    ? messagesError.errors[0].detail
                    : messagesError.message}
                </EmptyStateBody>
                <Button variant="primary" onClick={() => refetch()}>
                  {t('common.retry')}
                </Button>
              </EmptyState>
            ) : (
              <MessagesTable
                messages={messages}
                selectedMessage={selectedMessage}
                chosenColumns={chosenColumns}
                hasFilters={hasFilters}
                onSelectMessage={handleSelectMessage}
                onReset={handleReset}
                topicName={topicData?.attributes?.name || topicId || ''}
              />
            )}
          </PageSection>
        </DrawerContentBody>
      </DrawerContent>
    </Drawer>

    {showColumnsModal && (
      <ColumnsModal
        chosenColumns={chosenColumns}
        onConfirm={handleColumnsConfirm}
        onCancel={() => setShowColumnsModal(false)}
      />
    )}
    </>
  );
}