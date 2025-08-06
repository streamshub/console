import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

import { KafkaConnectParams } from "../../../../kafka-connect/kafkaConnect.params";
import RichText from "@/components/RichText";

export default function KafkaConnectClustersActiveBreadcrumbPage({
  params: { kafkaId, clusterId },
}: {
  params: KafkaConnectParams;
}) {
  return (
    <KafkaConnectClustersActiveBreadcrumb
      kafkaId={kafkaId}
      clusterId={clusterId}
    />
  );
}

function KafkaConnectClustersActiveBreadcrumb({
  kafkaId,
  clusterId,
}: {
  kafkaId: string;
  clusterId: string;
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
        to={`/kafka/${kafkaId}/kafka-connect`}
        showDivider
      >
        {t("breadcrumbs.Kafka_connect")}
      </BreadcrumbItem>
      <BreadcrumbItem
        key="connect-cluster"
        to={`/kafka/${kafkaId}/kafka-connect/connect-cluster`}
        showDivider
      >
        {t("breadcrumbs.connect_cluster")}
      </BreadcrumbItem>
      <BreadcrumbItem key={"cgm"} showDivider={true} isActive={true}>
        {decodeURIComponent(clusterId) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          clusterId
        )}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
