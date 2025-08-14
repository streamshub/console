import { getKafkaCluster } from "@/api/kafka/actions";
import { NavItemLink } from "@/components/Navigation/NavItemLink";
import { NavGroup, NavList } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { Suspense } from "react";

export function ClusterLinks({ kafkaId }: { kafkaId: string }) {
  const t = useTranslations();
  return (
    <NavList>
      <NavGroup
        title={
          (
            <Suspense>
              <ClusterName kafkaId={kafkaId} />
            </Suspense>
          ) as unknown as string
        }
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
        <NavItemLink url={`/kafka/${kafkaId}/kafka-connect`}>
          {t("AppLayout.kafka_connect")}
        </NavItemLink>
        {/*
      <NavItemLink url={`/kafka/${kafkaId}/service-registry`}>
        Service registry
      </NavItemLink>
*/}
        <NavItemLink url={`/kafka/${kafkaId}/consumer-groups`}>
          {t("AppLayout.consumer_groups")}
        </NavItemLink>
      </NavGroup>
    </NavList>
  );
}

async function ClusterName({ kafkaId }: { kafkaId: string }) {
  const cluster = (await getKafkaCluster(kafkaId))?.payload;
  return cluster?.attributes.name ?? `Cluster ${kafkaId}`;
}
