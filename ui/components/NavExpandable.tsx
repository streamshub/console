"use client";
import { usePathname } from "@/i18n/routing";
import {
  NavExpandable as PFNavExpendable,
  NavExpandableProps,
} from "@/libs/patternfly/react-core";
import { PropsWithChildren } from "react";

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
  const expanded = startExpanded || isActive;
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
