"use client";
import { NavExpandable } from "@/libs/patternfly/react-core";
import { useSelectedLayoutSegment } from "next/navigation";
import { PropsWithChildren, useCallback, useEffect, useState } from "react";

export function NavExpandableLink({
  url,
  title,
  children,
}: PropsWithChildren<{ url: string; title: string }>) {
  const segment = useSelectedLayoutSegment();
  const getIsActive = useCallback(() => {
    return url.startsWith(`/${segment}`);
  }, [url, segment]);
  const [active, setActive] = useState(getIsActive());
  const [expanded, setExpanded] = useState(active);
  useEffect(() => {
    const isActive = getIsActive();
    setActive(isActive);
    if (isActive) {
      setExpanded(isActive);
    }
  }, [getIsActive]);
  return (
    <NavExpandable
      title={title}
      isActive={active}
      onExpand={(_, expanded) => setExpanded(expanded)}
      isExpanded={expanded}
    >
      {children}
    </NavExpandable>
  );
}
