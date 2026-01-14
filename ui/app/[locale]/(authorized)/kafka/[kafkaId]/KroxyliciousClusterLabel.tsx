"use client";

import { Label, Tooltip } from "@/libs/patternfly/react-core";
import { InfoCircleIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function KroxyliciousClusterLabel() {
  const t = useTranslations();
  return (
    <Tooltip content={t("KroxyliciousCluster.tooltip")}>
      <Label
        isCompact
        color="blue"
        icon={<InfoCircleIcon />}
        className="pf-v6-u-ml-sm"
      >
        {t("KroxyliciousCluster.label")}
      </Label>
    </Tooltip>
  );
}
