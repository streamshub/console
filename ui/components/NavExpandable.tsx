"use client";
import { usePathname } from "@/navigation";
import {
  NavExpandable as PFNavExpendable,
  NavExpandableProps,
} from "@patternfly/react-core";
import { PropsWithChildren, useState } from "react";

export function NavExpandable({
  title,
  groupId,
  children,
  url,
  startExpanded,
}: PropsWithChildren<
  Pick<NavExpandableProps, "title" | "groupId"> & {
    url: string;
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
