"use client";
import { Label, Tooltip } from "@/libs/patternfly/react-core";
import { ServicesIcon } from "@/libs/patternfly/react-icons";

export function ManagedTopicLabel() {
  return (
    <Tooltip
      content={
        "Managed topics are created using the KafkaTopic custom resource and are created and updated in the Kafka cluster by the AMQ Streams Topic Operator."
      }
    >
      <Label
        isCompact={true}
        color={"gold"}
        icon={<ServicesIcon />}
        className={"pf-v5-u-ml-sm"}
      >
        Managed
      </Label>
    </Tooltip>
  );
}
