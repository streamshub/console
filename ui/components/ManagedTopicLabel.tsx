"use client";
import { Label, Tooltip } from "@/libs/patternfly/react-core";
import { ServicesIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function ManagedTopicLabel() {
  const t = useTranslations();
  return (
    <Tooltip content={t("ManagedTopicLabel.tooltip")}>
      <Label
        isCompact={true}
        color={"gold"}
        icon={<ServicesIcon />}
        className={"pf-v5-u-ml-sm"}
      >
        {t("ManagedTopicLabel.label")}
      </Label>
    </Tooltip>
  );
}
