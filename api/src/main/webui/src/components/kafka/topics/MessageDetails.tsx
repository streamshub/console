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
  ClipboardCopy,
  Title,
  Tooltip,
  CodeBlock,
  CodeBlockCode,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import { allExpanded, darkStyles, defaultStyles, JsonView } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import { KafkaRecord } from '@/api/types';
import { formatDateTime } from '@/utils/dateTime';
import { useTheme } from '@/components/app/ThemeProvider';
import { useSchemaContent } from '@/api/hooks/useSchemaContent';

interface MessageDetailsProps {
  message: KafkaRecord;
}

export function MessageDetails({ message }: MessageDetailsProps) {
  const { t } = useTranslation();
  const { isDarkMode } = useTheme();
  const jsonStyles = isDarkMode ? darkStyles : defaultStyles;
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

  const maybeJson = (value: string | null): { parsed: object; isJSON: boolean } => {
    if (!value) return { parsed: {}, isJSON: false };
    try {
      const parsed = JSON.parse(value);
      if (typeof parsed === 'object' && parsed !== null) {
        return { parsed, isJSON: true };
      }
    } catch {
      // not JSON
    }
    return { parsed: {}, isJSON: false };
  };

  const keyJson = maybeJson(message.attributes.key);
  const valueJson = maybeJson(message.attributes.value);
  const headers = Object.entries(message.attributes.headers ?? {});

  const keySchemaContentUrl = message.relationships.keySchema?.links?.content;
  const valueSchemaContentUrl = message.relationships.valueSchema?.links?.content;
  const { data: keySchemaContent } = useSchemaContent(keySchemaContentUrl);
  const { data: valueSchemaContent } = useSchemaContent(valueSchemaContentUrl);

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
          <DescriptionListTerm>
            {t('topics.messages.field.size')}{' '}
            <Tooltip content={t('topics.messages.tooltip.size')}>
              <HelpIcon />
            </Tooltip>
          </DescriptionListTerm>
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
              <ClipboardCopy
                isCode
                isReadOnly
                hoverTip="Copy"
                clickTip="Copied"
                variant={valueJson.isJSON ? 'inline' : 'expansion'}
                isExpanded={!valueJson.isJSON}
              >
                {message.attributes.value ?? '-'}
              </ClipboardCopy>
              {valueJson.isJSON && (
                <JsonView data={valueJson.parsed} shouldExpandNode={allExpanded} style={jsonStyles} />
              )}
              {message.relationships.valueSchema?.meta?.name && (
                <div style={{ marginTop: '1rem' }}>
                  <Title headingLevel="h4" size="md">
                    {t('topics.messages.schema')}
                  </Title>
                  <p>{message.relationships.valueSchema.meta.name}</p>
                  {valueSchemaContent && (
                    <CodeBlock style={{ marginTop: '0.5rem' }}>
                      <CodeBlockCode>{valueSchemaContent}</CodeBlockCode>
                    </CodeBlock>
                  )}
                </div>
              )}
            </div>
          </Tab>

          <Tab
            eventKey="key"
            title={<TabTitleText>{t('topics.messages.field.key')}</TabTitleText>}
          >
            <div style={{ padding: '1rem' }}>
              <ClipboardCopy
                isCode
                isReadOnly
                hoverTip="Copy"
                clickTip="Copied"
                variant={keyJson.isJSON ? 'inline' : 'expansion'}
                isExpanded={!keyJson.isJSON}
              >
                {message.attributes.key ?? '-'}
              </ClipboardCopy>
              {keyJson.isJSON && (
                <JsonView data={keyJson.parsed} shouldExpandNode={allExpanded} style={jsonStyles} />
              )}
              {message.relationships.keySchema?.meta?.name && (
                <div style={{ marginTop: '1rem' }}>
                  <Title headingLevel="h4" size="md">
                    {t('topics.messages.schema')}
                  </Title>
                  <p>{message.relationships.keySchema.meta.name}</p>
                  {keySchemaContent && (
                    <CodeBlock style={{ marginTop: '0.5rem' }}>
                      <CodeBlockCode>{keySchemaContent}</CodeBlockCode>
                    </CodeBlock>
                  )}
                </div>
              )}
            </div>
          </Tab>

          <Tab
            eventKey="headers"
            title={<TabTitleText>{t('topics.messages.field.headers')}</TabTitleText>}
          >
            <div style={{ padding: '1rem' }}>
              {headers.length === 0 ? (
                <span>-</span>
              ) : (
                <DescriptionList isHorizontal isCompact>
                  {headers.map(([k, v]) => (
                    <DescriptionListGroup key={k}>
                      <DescriptionListTerm>{k}</DescriptionListTerm>
                      <DescriptionListDescription>{String(v)}</DescriptionListDescription>
                    </DescriptionListGroup>
                  ))}
                </DescriptionList>
              )}
            </div>
          </Tab>
        </Tabs>
      </div>
    </DrawerPanelBody>
  );
}