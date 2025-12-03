import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { KafkaParams } from "../../kafka.params";

export default function KafkaUsersActiveBreadcrumbPage({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return <KafkaUsersActiveBreadcrumb kafkaId={kafkaId} />;
}

function KafkaUsersActiveBreadcrumb({ kafkaId }: { kafkaId: string }) {
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
      <BreadcrumbItem showDivider={true}>
        {t("breadcrumbs.kafka_users")}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
