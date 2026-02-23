import { ClusterDetail } from "@/api/kafka/schema";
import { NavItemLink } from "@/components/Navigation/NavItemLink";
import {
  NavGroup,
  NavList,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { Suspense } from "react";
import { KroxyliciousClusterLabel } from "./KroxyliciousClusterLabel";

export function ClusterLinks({ kafkaDetail }: { kafkaDetail: ClusterDetail }) {
  const t = useTranslations();
  const kafkaId = kafkaDetail.id;

  return (
    <NavList>
      <NavGroup
        title={
          (
            <Suspense>
              <ClusterName
                kafkaName={kafkaDetail.attributes.name}
                kind={kafkaDetail.meta?.kind}
              />
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
        <NavItemLink url={`/kafka/${kafkaId}/kafka-users`}>
          {t("AppLayout.kafka_users")}
        </NavItemLink>

        {/*
      <NavItemLink url={`/kafka/${kafkaId}/service-registry`}>
        Service registry
      </NavItemLink>
*/}
        <NavItemLink url={`/kafka/${kafkaId}/groups`}>
          {t("AppLayout.consumer_groups")}
        </NavItemLink>
      </NavGroup>
    </NavList>
  );
}

function ClusterName({
  kafkaName,
  kind,
}: {
  kafkaName: string;
  kind?: string | null;
}) {
  const isKroxy = kind === "virtualkafkaclusters.kroxylicious.io";

  return (
    <>
      {`Cluster ${kafkaName}`}
      {isKroxy && <KroxyliciousClusterLabel />}
    </>
  );
}
