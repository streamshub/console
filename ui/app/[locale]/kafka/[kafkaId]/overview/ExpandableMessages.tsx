"use client";
import { Label, LabelGroup, Title } from "@/libs/patternfly/react-core";
import {
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from "@/libs/patternfly/react-icons";
import { ExpandableSection } from "@patternfly/react-core";
import { PropsWithChildren, useState } from "react";

export function ExpandableMessages({
  warnings,
  dangers,
  children,
}: PropsWithChildren<{
  warnings: number;
  dangers: number;
}>) {
  const [showMessages, setShowMessages] = useState(warnings + dangers > 0);
  return (
    <ExpandableSection
      isExpanded={showMessages}
      onToggle={(_, isOpen) => setShowMessages(isOpen)}
      toggleContent={
        <Title headingLevel={"h3"} className={"pf-v5-u-font-size-sm"}>
          Cluster errors and warnings&nbsp;
          <LabelGroup>
            <Label color={"red"} isCompact={true}>
              <ExclamationCircleIcon /> {dangers}
            </Label>
            <Label color={"gold"} isCompact={true}>
              <ExclamationTriangleIcon /> {warnings}
            </Label>
          </LabelGroup>
        </Title>
      }
    >
      {children}
    </ExpandableSection>
  );
}
