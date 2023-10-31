"use client";
import {
  NavExpandable as PFNavExpendable,
  NavExpandableProps,
} from "@patternfly/react-core";
import { Route } from "next";
import { usePathname } from "next/navigation";
import { PropsWithChildren, useState } from "react";

export function NavExpandable<T extends string>({
  title,
  groupId,
  children,
  url,
  startExpanded,
}: PropsWithChildren<
  Pick<NavExpandableProps, "title" | "groupId"> & {
    url: Route<T> | URL;
    startExpanded: boolean;
  }
>) {
  const pathname = usePathname();
  const isActive = pathname.startsWith(url.toString());
  const [expanded, setExpanded] = useState(startExpanded || isActive);
  return (
    <PFNavExpendable
      title={title}
      groupId={groupId}
      isActive={isActive}
      isExpanded={expanded}
    >
      {children}
    </PFNavExpendable>
  );
}
