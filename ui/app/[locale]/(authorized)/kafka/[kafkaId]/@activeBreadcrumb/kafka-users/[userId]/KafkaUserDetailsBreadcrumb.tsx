"use client";

import RichText from "@/components/RichText";
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function KafkaUserDetailsBreadcrumb({
  kafkaId,
  name,
}: {
  kafkaId: string;
  name: string;
}) {
  const t = useTranslations();

  return (
    <Breadcrumb>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t("breadcrumbs.view_all_kafka_clusters")}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem
        key="overview"
        to={`/kafka/${kafkaId}/overview`}
        showDivider
      >
        {t("breadcrumbs.overview")}
      </BreadcrumbItem>
      <BreadcrumbItem
        key="kafka-connect"
        to={`/kafka/${kafkaId}/kafka-users`}
        showDivider={true}
      >
        {t("breadcrumbs.kafka_users")}
      </BreadcrumbItem>
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {decodeURIComponent(name) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          name
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
