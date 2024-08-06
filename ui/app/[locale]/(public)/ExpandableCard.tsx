"use client";
import {
  Card,
  CardExpandableContent,
  CardHeader,
  CardTitle,
} from "@/libs/patternfly/react-core";
import { PropsWithChildren, ReactNode, useState } from "react";

export function ExpandableCard({
  title,
  collapsedTitle,
  isCompact,
  children,
}: PropsWithChildren<{
  title: ReactNode;
  collapsedTitle?: ReactNode;
  isCompact?: boolean;
}>) {
  const [expanded, setExpanded] = useState(true);
  const titleNode =
    typeof title === "string" ? <CardTitle>{title}</CardTitle> : title;
  return (
    <Card isExpanded={expanded} isCompact={isCompact}>
      <CardHeader onExpand={() => setExpanded((e) => !e)}>
        {expanded && titleNode}
        {!expanded && (collapsedTitle || titleNode)}
      </CardHeader>
      <CardExpandableContent>{children}</CardExpandableContent>
    </Card>
  );
}
