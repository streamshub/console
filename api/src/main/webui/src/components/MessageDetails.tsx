/**
 * Message Details Component
 * Displays detailed information about a selected Kafka message
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  DrawerPanelBody,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Tabs,
  Tab,
  TabTitleText,
  CodeBlock,
  CodeBlockCode,
  Title,
} from '@patternfly/react-core';
import { KafkaRecord } from '../api/types';
import { formatDateTime } from '../utils/dateTime';

interface MessageDetailsProps {
  message: KafkaRecord;
}

export function MessageDetails({ message }: MessageDetailsProps) {
  const { t } = useTranslation();
  const [activeTabKey, setActiveTabKey] = useState<string | number>('value');

  const formatTimestampLocal = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
    });
  };

  const formatTimestampUTC = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
      timeZone: 'UTC',
    });
  };

  const formatBytes = (bytes?: number): string => {
    if (bytes === undefined || bytes === null) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const formatEpoch = (timestamp: string): string => {
    try {
      return String(Math.floor(new Date(timestamp).getTime() / 1000));
    } catch {
      return '-';
    }
  };

  const tryFormatJSON = (value: string | null): { formatted: string; isJSON: boolean } => {
    if (!value) return { formatted: '-', isJSON: false };
    try {
      const parsed = JSON.parse(value);
      return { formatted: JSON.stringify(parsed, null, 2), isJSON: true };
    } catch {
      return { formatted: value, isJSON: false };
    }
  };

  const keyFormatted = tryFormatJSON(message.attributes.key);
  const valueFormatted = tryFormatJSON(message.attributes.value);
  const headersFormatted = JSON.stringify(message.attributes.headers, null, 2);

  return (
    <DrawerPanelBody>
      <DescriptionList isHorizontal isCompact>
        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.partition')}</DescriptionListTerm>
          <DescriptionListDescription>
            {message.attributes.partition}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.offset')}</DescriptionListTerm>
          <DescriptionListDescription>
            {message.attributes.offset}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.size')}</DescriptionListTerm>
          <DescriptionListDescription>
            {formatBytes(message.attributes.size)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.timestamp')}</DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestampLocal(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.timestampUTC')}</DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestampUTC(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.epoch')}</DescriptionListTerm>
          <DescriptionListDescription>
            {formatEpoch(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.keyFormat')}</DescriptionListTerm>
          <DescriptionListDescription>
            {message.relationships.keySchema?.meta?.artifactType || 'Plain'}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>{t('topics.messages.field.valueFormat')}</DescriptionListTerm>
          <DescriptionListDescription>
            {message.relationships.valueSchema?.meta?.artifactType || 'Plain'}
          </DescriptionListDescription>
        </DescriptionListGroup>
      </DescriptionList>

      <div style={{ marginTop: '1rem' }}>
        <Tabs
          activeKey={activeTabKey}
          onSelect={(_, tabKey) => setActiveTabKey(tabKey)}
        >
          <Tab
            eventKey="value"
            title={<TabTitleText>{t('topics.messages.field.value')}</TabTitleText>}
          >
            <div style={{ padding: '1rem' }}>
              <CodeBlock>
                <CodeBlockCode>{valueFormatted.formatted}</CodeBlockCode>
              </CodeBlock>
              {message.relationships.valueSchema?.meta?.name && (
                <div style={{ marginTop: '1rem' }}>
                  <Title headingLevel="h4" size="md">
                    {t('topics.messages.schema')}
                  </Title>
                  <p>{message.relationships.valueSchema.meta.name}</p>
                </div>
              )}
            </div>
          </Tab>

          <Tab
            eventKey="key"
            title={<TabTitleText>{t('topics.messages.field.key')}</TabTitleText>}
          >
            <div style={{ padding: '1rem' }}>
              <CodeBlock>
                <CodeBlockCode>{keyFormatted.formatted}</CodeBlockCode>
              </CodeBlock>
              {message.relationships.keySchema?.meta?.name && (
                <div style={{ marginTop: '1rem' }}>
                  <Title headingLevel="h4" size="md">
                    {t('topics.messages.schema')}
                  </Title>
                  <p>{message.relationships.keySchema.meta.name}</p>
                </div>
              )}
            </div>
          </Tab>

          <Tab
            eventKey="headers"
            title={<TabTitleText>{t('topics.messages.field.headers')}</TabTitleText>}
          >
            <div style={{ padding: '1rem' }}>
              <CodeBlock>
                <CodeBlockCode>{headersFormatted}</CodeBlockCode>
              </CodeBlock>
            </div>
          </Tab>
        </Tabs>
      </div>
    </DrawerPanelBody>
  );
}