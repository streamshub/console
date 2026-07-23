/**
 * usePageTitle Hook
 *
 * Sets the browser tab/window title in the format:
 *   "Page Name | StreamsHub Console"
 *
 * When called with no argument (from KafkaLayout and HomePage), the title is
 * derived from the `handle.title` of the deepest matched route that declares
 * one.  Detail pages that have a dynamically-fetched name (e.g. topic name,
 * connector name) call usePageTitle(fetchedName) to override the title once
 * the data is available.
 *
 * When no page title can be determined the fallback is just "StreamsHub Console".
 */

import { useEffect } from 'react';
import { useMatches } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { RouteHandle } from '@/routes';

export function usePageTitle(override?: string) {
  const { t } = useTranslation();
  const matches = useMatches();

  // Walk matches from deepest to shallowest to find the most specific title.
  const routeTitle = [...matches]
    .reverse()
    .map((m) => (m.handle as RouteHandle | undefined)?.title)
    .find((titleFn) => typeof titleFn === 'function')?.(t);

  const page = override ?? routeTitle;
  const appTitle = t('common.title');

  useEffect(() => {
    document.title = page ? `${page} | ${appTitle}` : appTitle;
  }, [page, appTitle]);
}
