import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { KafkaParams } from "../../kafka.params";

export default function ConsumerGroupsActiveBreadcrumbPage({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  return <ConsumerGroupsActiveBreadcrumb kafkaId={kafkaId} />;
}

function ConsumerGroupsActiveBreadcrumb({ kafkaId }: { kafkaId: string }) {
  const t = useTranslations("breadcrumbs");

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
      <BreadcrumbItem showDivider isActive={true}>
        {t("consumer_groups")}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
