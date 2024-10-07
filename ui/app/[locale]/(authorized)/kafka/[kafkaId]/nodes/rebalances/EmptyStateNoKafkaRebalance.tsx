import {
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  EmptyStateIcon,
} from "@/libs/patternfly/react-core";
import { CubesIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function EmptyStateNoKafkaRebalance({}: {}) {
  const t = useTranslations("Rebalancing");
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText={t("no_kafka_cluster_rebalances_found")}
        headingLevel="h4"
        icon={<EmptyStateIcon icon={CubesIcon} />}
      />
      <EmptyStateBody>
        {t("no_kafka_cluster_rebalances_found_description")}
      </EmptyStateBody>
    </EmptyState>
  );
}
