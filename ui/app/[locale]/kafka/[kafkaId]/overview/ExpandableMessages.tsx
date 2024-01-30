"use client";
import { Label, LabelGroup, Title, Tooltip } from "@/libs/patternfly/react-core";
import {
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon
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
          Cluster errors and warnings{" "}
          <Tooltip
              content={
                "Issues encountered in the Kafka cluster. Investigate and address these issues to ensure continued operation of the cluster."
              }
            >
              <HelpIcon />
          </Tooltip>
            {" "}
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
