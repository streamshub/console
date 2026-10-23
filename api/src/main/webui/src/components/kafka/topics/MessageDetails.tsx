/**
 * Message Details Component
 * Displays detailed information about a selected Kafka message
 */

import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Button,
  ClipboardCopy,
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  DrawerPanelBody,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  Progress,
  ProgressMeasureLocation,
  Tab,
  Tabs,
  TabTitleText,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import { HelpIcon } from '@patternfly/react-icons';
import {
  allExpanded,
  darkStyles,
  defaultStyles,
  JsonView,
} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import { KafkaRecord } from '@/api/types';
import { formatDateTime } from '@/utils/dateTime';
import { useTheme } from '@/components/app/ThemeProvider';
import { useSchemaContent } from '@/api/hooks/useSchemaContent';

interface MessageDetailsProps {
  message: KafkaRecord;
}

// Number of base64 characters decoded per animation-frame slice.
// Each base64 char represents 6 bits; 65536 chars ≈ 48 KB of output per slice,
// keeping each slice well under the ~10 ms frame budget on low-end hardware.
const DECODE_CHUNK_SIZE = 65_536;

const MESSAGE_TIMESTAMP_FORMAT = 'yyyy-MM-dd HH:mm:ss.SSSXXX';

export function MessageDetails({ message }: MessageDetailsProps) {
  const { t } = useTranslation();
  const { isDarkMode } = useTheme();
  const jsonStyles = isDarkMode ? darkStyles : defaultStyles;
  const [activeTabKey, setActiveTabKey] = useState<string | number>('value');

  // Download progress modal state
  const [downloadFilename, setDownloadFilename] = useState<string | null>(null);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const cancelledRef = useRef(false);

  const formatTimestampLocal = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
      format: MESSAGE_TIMESTAMP_FORMAT,
    });
  };

  const formatTimestampUTC = (timestamp: string): string => {
    return formatDateTime({
      value: timestamp,
      timeZone: 'UTC',
      format: MESSAGE_TIMESTAMP_FORMAT,
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

  const maybeJson = (
    value: string | null
  ): { parsed: object; isJSON: boolean } => {
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

  const isBinaryKey = message.meta?.content?.key?.type === 'application/octet-stream';
  const keyMeta = message.meta?.content?.key;

  const isBinaryValue = message.meta?.content?.value?.type === 'application/octet-stream';
  const valueMeta = message.meta?.content?.value;

  // Skip the JSON parse attempt for binary fields — they contain base64, not JSON.
  const keyJson = isBinaryKey ? { parsed: {}, isJSON: false } : maybeJson(message.attributes.key);
  const valueJson = isBinaryValue ? { parsed: {}, isJSON: false } : maybeJson(message.attributes.value);
  const headers = Object.entries(message.attributes.headers ?? {});

  /**
   * Decode a base64 string to a Uint8Array in DECODE_CHUNK_SIZE slices,
   * yielding to the browser between each slice via requestAnimationFrame so the
   * UI stays responsive and progress updates are painted.  Returns null if the
   * operation was cancelled by the user.
   */
  const decodeBase64Async = (base64Value: string): Promise<Uint8Array | null> => {
    return new Promise((resolve, reject) => {
      // atob validates the alphabet and padding; an invalid string throws DOMException.
      let decoded: string;
      try {
        decoded = atob(base64Value);
      } catch {
        reject(new Error(t('topics.messages.downloadErrorBody')));
        return;
      }

      const totalLen = decoded.length;
      const bytes = new Uint8Array(totalLen);
      let offset = 0;

      const processChunk = () => {
        // Check cancellation before each chunk
        if (cancelledRef.current) {
          resolve(null);
          return;
        }

        const end = Math.min(offset + DECODE_CHUNK_SIZE, totalLen);
        for (let i = offset; i < end; i++) {
          bytes[i] = decoded.charCodeAt(i);
        }
        offset = end;

        const progress = totalLen === 0 ? 100 : Math.round((offset / totalLen) * 100);
        setDownloadProgress(progress);

        if (offset < totalLen) {
          requestAnimationFrame(processChunk);
        } else {
          resolve(bytes);
        }
      };

      requestAnimationFrame(processChunk);
    });
  };

  const handleDownload = async (fieldName: string, base64Value: string | null) => {
    if (!base64Value) return;

    // Sanitise fieldName: only allow alphanumerics, hyphens and underscores to
    // prevent path-traversal sequences (e.g. "../../etc/passwd") reaching the
    // browser's showSaveFilePicker suggestedName or the anchor download attribute.
    const safeFieldName = fieldName.replace(/[^a-zA-Z0-9_-]/g, '_');
    const filename = `message-p${message.attributes.partition}-o${message.attributes.offset}-${safeFieldName}.bin`;

    // Open the progress modal
    cancelledRef.current = false;
    setDownloadProgress(0);
    setDownloadError(null);
    setDownloadFilename(filename);

    try {
      // Decode asynchronously with per-chunk progress updates
      const bytes = await decodeBase64Async(base64Value);

      // User cancelled during decode
      if (bytes === null) {
        setDownloadFilename(null);
        return;
      }

      // `bytes.buffer` is typed as `ArrayBufferLike` (which includes SharedArrayBuffer)
      // but Blob only accepts `ArrayBuffer`. The buffer produced by `new Uint8Array(n)`
      // is always a plain ArrayBuffer, so this cast is safe.
      const blob = new Blob([bytes.buffer as ArrayBuffer], { type: 'application/octet-stream' });

      // Close modal before opening the native save dialog
      setDownloadFilename(null);

      // Use the File System Access API save dialog when available (Chromium/Edge).
      // Falls back to the silent anchor-click download for Firefox/Safari.
      if ('showSaveFilePicker' in window) {
        try {
          const fileHandle = await (window as unknown as {
            showSaveFilePicker: (opts: object) => Promise<{ createWritable: () => Promise<{ write: (b: Blob) => Promise<void>; close: () => Promise<void> }> }>;
          }).showSaveFilePicker({
            suggestedName: filename,
            types: [{ description: 'Binary file', accept: { 'application/octet-stream': ['.bin'] } }],
          });
          const writable = await fileHandle.createWritable();
          await writable.write(blob);
          await writable.close();
          return;
        } catch (e) {
          // User cancelled the dialog — do nothing.
          if (e instanceof DOMException && e.name === 'AbortError') return;
          // Any other error: fall through to the anchor fallback.
        }
      }

      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (e) {
      const message = e instanceof Error ? e.message : t('topics.messages.downloadErrorBody');
      setDownloadError(message);
    }
  };

  const handleCancelDownload = () => {
    cancelledRef.current = true;
    setDownloadFilename(null);
    setDownloadError(null);
  };

  const keySchemaContentUrl = message.relationships.keySchema?.links?.content;
  const valueSchemaContentUrl =
    message.relationships.valueSchema?.links?.content;
  const { data: keySchemaContent } = useSchemaContent(keySchemaContentUrl);
  const { data: valueSchemaContent } = useSchemaContent(valueSchemaContentUrl);

  return (
    <DrawerPanelBody>
      {/* Binary decode progress modal */}
      <Modal
        variant={ModalVariant.small}
        isOpen={downloadFilename !== null}
        onClose={handleCancelDownload}
        aria-label={t('topics.messages.downloadProgressTitle')}
      >
        <ModalHeader title={t('topics.messages.downloadProgressTitle')} />
        <ModalBody>
          {downloadError ? (
            <Alert
              variant="danger"
              isInline
              title={t('topics.messages.downloadErrorTitle')}
            >
              {downloadError}
            </Alert>
          ) : (
            <Progress
              value={downloadProgress}
              title={t('topics.messages.downloadProgressLabel', { filename: downloadFilename ?? '' })}
              measureLocation={ProgressMeasureLocation.outside}
            />
          )}
        </ModalBody>
        <ModalFooter>
          <Button variant="link" onClick={handleCancelDownload}>
            {t('topics.messages.downloadProgressCancel')}
          </Button>
        </ModalFooter>
      </Modal>

      <DescriptionList isHorizontal isCompact>
        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.partition')}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {message.attributes.partition}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.offset')}
          </DescriptionListTerm>
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
          <DescriptionListTerm>
            {t('topics.messages.field.timestamp')}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestampLocal(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.timestampUTC')}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestampUTC(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.epoch')}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {formatEpoch(message.attributes.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.keyFormat')}
          </DescriptionListTerm>
          <DescriptionListDescription>
            {message.relationships.keySchema?.meta?.artifactType || 'Plain'}
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>
            {t('topics.messages.field.valueFormat')}
          </DescriptionListTerm>
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
            title={
              <TabTitleText>{t('topics.messages.field.value')}</TabTitleText>
            }
          >
            <div style={{ padding: '1rem' }}>
              {isBinaryValue ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                  <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                    {valueMeta?.omitted
                      ? t('topics.messages.binaryDataOmitted')
                      : t('topics.messages.binaryDataNotDisplayed')}
                  </span>
                  {!valueMeta?.omitted && message.attributes.value && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleDownload('value', message.attributes.value)}
                    >
                      {t('topics.messages.downloadButton')}
                    </Button>
                  )}
                </div>
              ) : message.attributes.value === null ? (
                <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                  {t('topics.messages.noValue')}
                </span>
              ) : (
                <>
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
                    <JsonView
                      data={valueJson.parsed}
                      shouldExpandNode={allExpanded}
                      style={jsonStyles}
                    />
                  )}
                </>
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
            title={
              <TabTitleText>{t('topics.messages.field.key')}</TabTitleText>
            }
          >
            <div style={{ padding: '1rem' }}>
              {isBinaryKey ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                  <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                    {keyMeta?.omitted
                      ? t('topics.messages.binaryDataOmitted')
                      : t('topics.messages.binaryDataNotDisplayed')}
                  </span>
                  {!keyMeta?.omitted && message.attributes.key && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleDownload('key', message.attributes.key)}
                    >
                      {t('topics.messages.downloadButton')}
                    </Button>
                  )}
                </div>
              ) : message.attributes.key === null ? (
                <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                  {t('topics.messages.noKey')}
                </span>
              ) : (
                <>
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
                    <JsonView
                      data={keyJson.parsed}
                      shouldExpandNode={allExpanded}
                      style={jsonStyles}
                    />
                  )}
                </>
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
            title={
              <TabTitleText>{t('topics.messages.field.headers')}</TabTitleText>
            }
          >
            <div style={{ padding: '1rem' }}>
              {headers.length === 0 ? (
                <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                  {t('topics.messages.noHeaders')}
                </span>
              ) : (
                <DescriptionList isHorizontal isCompact>
                  {headers.map(([k, v]) => {
                    const headerMeta = message.meta?.content?.headers?.[k];
                    const isBinaryHeader = headerMeta?.type === 'application/octet-stream';
                    return (
                      <DescriptionListGroup key={k}>
                        <DescriptionListTerm>{k}</DescriptionListTerm>
                        <DescriptionListDescription>
                          {isBinaryHeader ? (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                              <span style={{ fontStyle: 'italic', color: 'var(--pf-t--global--text--color--subtle)' }}>
                                {headerMeta?.omitted
                                  ? t('topics.messages.binaryDataOmitted')
                                  : t('topics.messages.binaryDataNotDisplayed')}
                              </span>
                              {!headerMeta?.omitted && v && (
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  onClick={() => handleDownload(`header-${k}`, String(v))}
                                >
                                  {t('topics.messages.downloadButton')}
                                </Button>
                              )}
                            </div>
                          ) : (
                            String(v)
                          )}
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    );
                  })}
                </DescriptionList>
              )}
            </div>
          </Tab>
        </Tabs>
      </div>
    </DrawerPanelBody>
  );
}
