"use client";

import RichText from "@/components/RichText";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function ConnectorBreadcrumb({
  kafkaId,
  connectorName,
}: {
  kafkaId: string;
  connectorName: string;
}) {
  const t = useTranslations();

  return (
    <Breadcrumb>
      <BreadcrumbItem key="connectors" showDivider>
        {t("breadcrumbs.connectors")}
      </BreadcrumbItem>
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {decodeURIComponent(connectorName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          connectorName
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
