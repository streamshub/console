import { NavItemLink } from "@/components/Navigation/NavItemLink";
import { NavGroup, NavList } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { ClusterDetail } from "@/api/kafka/schema";

export function ClusterLinks({ kafkaCluster }: { kafkaCluster: ClusterDetail; }) {
  const t = useTranslations();
  const kafkaId = kafkaCluster.id;

  return (
    <NavList>
      <NavGroup
        title={ kafkaCluster?.attributes.name ?? `Cluster ${kafkaId}` }
      >
        <NavItemLink url={`/kafka/${kafkaId}/overview`}>
          {t("AppLayout.cluster_overview")}
        </NavItemLink>
        <NavItemLink url={`/kafka/${kafkaId}/topics`}>
          {t("AppLayout.topics")}
        </NavItemLink>
        <NavItemLink url={`/kafka/${kafkaId}/nodes`}>
          {t("AppLayout.brokers")}
        </NavItemLink>
        <NavItemLink url={`/kafka/${kafkaId}/consumer-groups`}>
          {t("AppLayout.consumer_groups")}
        </NavItemLink>
      </NavGroup>
    </NavList>
  );
}
