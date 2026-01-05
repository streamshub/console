import { use } from "react";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { BreadcrumbLink } from "@/components/Navigation/BreadcrumbLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export default function TopicsActiveBreadcrumb(
  props: {
    params: Promise<KafkaParams>;
  }
) {
  const params = use(props.params);

  const {
    kafkaId
  } = params;

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
      <BreadcrumbLink
        key={"topics"}
        href={`/kafka/${kafkaId}/topics`}
        showDivider={true}
      >
        {t("topics")}
      </BreadcrumbLink>
      <BreadcrumbItem key={"create-topic"} showDivider={true}>
        {t("create_topic")}
      </BreadcrumbItem>
    </Breadcrumb>
  );
}
