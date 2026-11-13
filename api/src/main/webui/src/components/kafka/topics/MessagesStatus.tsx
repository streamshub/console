/**
 * Messages Status Component
 * A summary line shown above the messages table.
 *
 * Left: how many messages are shown. When a search is active this also shows
 * how many records were scanned to find them.
 * Right: the state of the retrieval - running (with a way to cancel it),
 * or how it ended: complete, stopped, or ended early with partial results.
 */

import { useTranslation } from 'react-i18next';
import {
  Button,
  Content,
  Flex,
  FlexItem,
  Progress,
  ProgressMeasureLocation,
  ProgressSize,
  Spinner,
  Tooltip,
} from '@patternfly/react-core';
import { ScanState } from '@/api/hooks/useMessages';

/** Format a duration in milliseconds as m:ss (or h:mm:ss for an hour or more). */
function formatDuration(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = String(totalSeconds % 60).padStart(2, '0');
  return hours > 0
    ? `${hours}:${String(minutes).padStart(2, '0')}:${seconds}`
    : `${minutes}:${seconds}`;
}

interface MessagesStatusProps {
  /** Number of messages returned so far */
  count: number;
  /** How far the server has got in scanning the topic */
  scan: ScanState;
  /** A search term is active; the scanned count and progress are only shown then */
  isSearching: boolean;
  isStreaming: boolean;
  /** The retrieval was cancelled before the server finished */
  isStopped: boolean;
  onCancel: () => void;
}

export function MessagesStatus({
  count,
  scan,
  isSearching,
  isStreaming,
  isStopped,
  onCancel,
}: MessagesStatusProps) {
  const { t, i18n } = useTranslation();
  const formatNumber = (value: number) =>
    new Intl.NumberFormat(i18n.language).format(value);

  const summary = isSearching
    ? t('topics.messages.matchesSummary', {
        count,
        formatted: formatNumber(count),
        scanned: formatNumber(scan.scanned),
      })
    : t('topics.messages.messagesCount', {
        count,
        formatted: formatNumber(count),
      });

  // Once retrieval has finished, the last elapsed time reported is how long it took
  const showDuration = !isStreaming && scan.elapsed !== undefined;

  // Time the retrieval has been running and the maximum time left
  const runningTime =
    scan.elapsed !== undefined && scan.remaining !== undefined ? (
      <FlexItem>
        <Tooltip content={t('topics.messages.searchTimeTooltip')}>
          {/* Focusable so the tooltip is available to keyboard users */}
          <span tabIndex={0}>
            <Content component="small">
              {t('topics.messages.searchTime', {
                elapsed: formatDuration(scan.elapsed),
                remaining: formatDuration(scan.remaining),
              })}
            </Content>
          </span>
        </Tooltip>
      </FlexItem>
    ) : null;

  let state: React.ReactNode = null;

  if (isStreaming) {
    state = (
      <Flex
        alignItems={{ default: 'alignItemsCenter' }}
        spaceItems={{ default: 'spaceItemsSm' }}
        flexWrap={{ default: 'nowrap' }}
      >
        <FlexItem>
          {isSearching ? (
            // Only an estimate of how much of the topic is scanned, so no
            // percentage is shown, just the bar
            <Flex
              alignItems={{ default: 'alignItemsCenter' }}
              spaceItems={{ default: 'spaceItemsMd' }}
              flexWrap={{ default: 'nowrap' }}
            >
              {runningTime}
              <FlexItem>
                <div style={{ width: '12rem' }}>
                  <Progress
                    value={scan.progress}
                    size={ProgressSize.sm}
                    measureLocation={ProgressMeasureLocation.none}
                    aria-label={t('topics.messages.searching')}
                  />
                </div>
              </FlexItem>
            </Flex>
          ) : (
            <Flex
              alignItems={{ default: 'alignItemsCenter' }}
              spaceItems={{ default: 'spaceItemsSm' }}
              flexWrap={{ default: 'nowrap' }}
            >
              <FlexItem>
                <Spinner size="md" aria-label={t('topics.messages.retrieving')} />
              </FlexItem>
              <FlexItem>
                <Content component="small">
                  {t('topics.messages.retrieving')}
                </Content>
              </FlexItem>
              {runningTime}
            </Flex>
          )}
        </FlexItem>
        <FlexItem>
          <Button
            variant="secondary"
            size="sm"
            onClick={onCancel}
            aria-label={t('topics.messages.cancelRetrievalAriaLabel')}
          >
            {t('topics.messages.cancelRetrieval')}
          </Button>
        </FlexItem>
      </Flex>
    );
  } else if (isStopped) {
    state = (
      <Content component="small">{t('topics.messages.scanStopped')}</Content>
    );
  } else if (isSearching) {
    // Say how the server ended the scan, so partial results are not mistaken for complete ones
    const key =
      scan.endReason === 'limit'
        ? 'topics.messages.scanLimitReached'
        : scan.endReason === 'timeout'
          ? 'topics.messages.scanTimedOut'
          : scan.endReason === 'cancelled'
            ? 'topics.messages.scanStopped'
            : 'topics.messages.scanComplete';

    state = <Content component="small">{t(key)}</Content>;
  }

  return (
    <Flex
      justifyContent={{ default: 'justifyContentSpaceBetween' }}
      alignItems={{ default: 'alignItemsCenter' }}
      flexWrap={{ default: 'nowrap' }}
      // Fixed height so the line does not change size when the state changes
      style={{ minHeight: '2rem' }}
      data-testid="messages-status"
    >
      <FlexItem>
        <Content component="small" aria-live="polite">
          {showDuration
            ? t('topics.messages.summaryWithDuration', {
                summary,
                elapsed: formatDuration(scan.elapsed!),
              })
            : summary}
        </Content>
      </FlexItem>
      <FlexItem>{state}</FlexItem>
    </Flex>
  );
}
