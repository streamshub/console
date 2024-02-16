import { RefreshInterval, RefreshSelector } from "@/components/RefreshSelector";
import { useLayoutEffect, useState } from "react";
import { createPortal } from "react-dom";

export function ConnectedRefreshSelector({
  isRefreshing,
  isLive,
  refreshInterval,
  lastRefresh,
  onRefresh,
  onRefreshInterval,
  onToggleLive,
}: {
  isRefreshing: boolean;
  isLive: boolean;
  refreshInterval: RefreshInterval;
  lastRefresh: Date | undefined;
  onRefresh: () => void;
  onRefreshInterval: (interval: RefreshInterval) => void;
  onToggleLive: (enable: boolean) => void;
}) {
  const [container, setContainer] = useState<Element | undefined>();

  function seekContainer() {
    const el = document.getElementById("topic-header-portal");
    if (el) {
      setContainer(el);
    } else {
      setTimeout(seekContainer, 100);
    }
  }

  useLayoutEffect(seekContainer, []); // we want to run this just once

  return container
    ? createPortal(
        <RefreshSelector
          isRefreshing={isRefreshing}
          isLive={isLive}
          refreshInterval={refreshInterval}
          lastRefresh={lastRefresh}
          onRefresh={onRefresh}
          onToggleLive={onToggleLive}
          onIntervalChange={onRefreshInterval}
        />,
        container,
      )
    : null;
}
