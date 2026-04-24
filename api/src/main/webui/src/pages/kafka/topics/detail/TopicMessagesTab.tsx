/**
 * Topic Messages Tab - Shows messages in a topic with advanced search and detail drawer
 */

import { useState, useCallback, useEffect } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PageSection,
  Drawer,
  DrawerContent,
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
import { SearchIcon, ExclamationCircleIcon, ColumnsIcon } from '@patternfly/react-icons';
import { useMessages } from '@/api/hooks/useMessages';
import { useTopic } from '@/api/hooks/useTopics';
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
  const selectedMessageId = searchParams.get('selected');

  const [showColumnsModal, setShowColumnsModal] = useState(false);
  const [chosenColumns, setChosenColumns] = useState<Column[]>(defaultColumns);

  const [selectedMessage, setSelectedMessage] = useState<KafkaRecord | undefined>();

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

  // Handle message selection from URL
  useEffect(() => {
    if (selectedMessageId && messages.length > 0) {
      const [partStr, offsetStr] = selectedMessageId.split(':');
      const part = parseInt(partStr);
      const off = parseInt(offsetStr);
      const msg = messages.find(
        m => m.attributes.partition === part && m.attributes.offset === off
      );
      if (msg) {
        setSelectedMessage(msg);
      }
    } else if (!selectedMessageId) {
      setSelectedMessage(undefined);
    }
  }, [selectedMessageId, messages]);

  const handleSearch = useCallback((params: SearchParams) => {
    const newParams = new URLSearchParams();
    
    if (params.query) {
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
    
    setSearchParams(newParams);
  }, [setSearchParams]);

  const handleSelectMessage = useCallback((message: KafkaRecord) => {
    setSelectedMessage(message);
    const newParams = new URLSearchParams(searchParams);
    newParams.set('selected', `${message.attributes.partition}:${message.attributes.offset}`);
    setSearchParams(newParams);
  }, [searchParams, setSearchParams]);

  const handleDeselectMessage = useCallback(() => {
    setSelectedMessage(undefined);
    const newParams = new URLSearchParams(searchParams);
    newParams.delete('selected');
    setSearchParams(newParams);
  }, [searchParams, setSearchParams]);

  const handleReset = useCallback(() => {
    setSearchParams(new URLSearchParams());
  }, [setSearchParams]);

  // Load saved column preferences from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('message-browser-columns');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          setChosenColumns(parsed);
        }
      } catch {
        // Ignore parse errors
      }
    }
  }, []);

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

  // Error state
  if (messagesError) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <ExclamationCircleIcon />
          <Title headingLevel="h2" size="lg">
            {t('topics.messages.noDataTitle')}
          </Title>
          <EmptyStateBody>
            {messagesError.title || t('common.error')}
          </EmptyStateBody>
          <Button variant="primary" onClick={() => refetch()}>
            {t('common.retry')}
          </Button>
        </EmptyState>
      </PageSection>
    );
  }

  // No data state (no filters applied)
  const hasFilters = partition !== undefined || offset !== undefined ||
                     timestamp !== undefined || epoch !== undefined || query !== undefined;
  
  if (messages.length === 0 && !hasFilters) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <SearchIcon />
          <Title headingLevel="h2" size="lg">
            {t('topics.messages.noDataTitle')}
          </Title>
          <EmptyStateBody>
            {t('topics.messages.noDataBody')}
          </EmptyStateBody>
          <Button variant="primary" onClick={() => refetch()}>
            {t('topics.messages.noDataRefresh')}
          </Button>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <>
    <PageSection isFilled>
      <Drawer isExpanded={!!selectedMessage} isInline>
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

          <MessagesTable
            messages={messages}
            selectedMessage={selectedMessage}
            chosenColumns={chosenColumns}
            onSelectMessage={handleSelectMessage}
            onReset={handleReset}
            topicName={topicData?.attributes?.name || topicId || ''}
          />
        </DrawerContent>
      </Drawer>
    </PageSection>

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