"use client";

import RichText from "@/components/RichText";
import { Breadcrumb, BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function ConnectClusterBreadcrumb({
  kafkaId,
  connectClusterName,
}: {
  kafkaId: string;
  connectClusterName: string;
}) {
  const t = useTranslations();

  return (
    <Breadcrumb>
      <BreadcrumbItem
        key="connect_clusters"
        to={`/kafka/${kafkaId}/kafka-connect/connect-clusters`}
        showDivider
      >
        {t("breadcrumbs.connect_clusters")}
      </BreadcrumbItem>
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {decodeURIComponent(connectClusterName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          connectClusterName
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
