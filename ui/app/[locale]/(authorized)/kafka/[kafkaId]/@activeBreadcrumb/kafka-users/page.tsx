import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { getTranslations } from "next-intl/server";
import { KafkaParams } from "../../kafka.params";

export default async function KafkaUsersActiveBreadcrumbPage({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const { kafkaId } = await paramsPromise;
  return <KafkaUsersActiveBreadcrumb kafkaId={kafkaId} />;
}

async function KafkaUsersActiveBreadcrumb({ kafkaId }: { kafkaId: string }) {
  const t = await getTranslations();

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
