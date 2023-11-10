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
  children,
}: PropsWithChildren<{ title: ReactNode; collapsedTitle?: ReactNode }>) {
  const [expanded, setExpanded] = useState(true);
  const titleNode =
    typeof title === "string" ? <CardTitle>{title}</CardTitle> : title;
  return (
    <Card isExpanded={expanded}>
      <CardHeader onExpand={() => setExpanded((e) => !e)}>
        {expanded && titleNode}
        {!expanded && (collapsedTitle || titleNode)}
      </CardHeader>
      <CardExpandableContent>{children}</CardExpandableContent>
    </Card>
  );
}
