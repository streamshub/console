import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { getTranslations } from "next-intl/server";
import { KafkaParams } from "../../kafka.params";

export default async function TopicsActiveBreadcrumbPage({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const { kafkaId } = await paramsPromise;
  return <TopicsActiveBreadcrumb kafkaId={kafkaId} />;
}

async function TopicsActiveBreadcrumb({ kafkaId }: { kafkaId: string }) {
  const t = await getTranslations("breadcrumbs");

  return (
    <Breadcrumb>
      <BreadcrumbItem key="home" to="/" showDivider>
        <Tooltip content={t("view_all_kafka_clusters")}>
          <HomeIcon />
        </Tooltip>
      </BreadcrumbItem>
      <BreadcrumbItem
        key="overview"
        to={`/kafka/${kafkaId}/overview`}
        showDivider
      >
        {t("overview")}
      </BreadcrumbItem>
      <BreadcrumbItem showDivider>{t("topics")}</BreadcrumbItem>
    </Breadcrumb>
  );
}
