"use client";
import { Label, Tooltip } from "@/libs/patternfly/react-core";
import { ServicesIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function ManagedConnectorLabel() {
  const t = useTranslations("KafkaConnect");
  return (
    <Tooltip content={t("ManagedConnectorLabel.tooltip")}>
      <Label
        isCompact={true}
        color={"yellow"}
        icon={<ServicesIcon />}
        className={"pf-v6-u-ml-sm"}
      >
        {t("ManagedConnectorLabel.label")}
      </Label>
    </Tooltip>
  );
}
