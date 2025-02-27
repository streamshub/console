import { getNodes } from "@/api/nodes/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/DistributionChart";
import {
  Node,
  NodeStatus,
  NodesTable,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/NodesTable";
import { Alert, PageSection } from "@/libs/patternfly/react-core";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("nodes.title")} | ${t("common.title")}`,
  };
}

export default function NodesPage({ params }: { params: KafkaParams }) {
  return (
    <Suspense fallback={null}>
      <ConnectedNodes params={params} />
    </Suspense>
  );
}

async function ConnectedNodes({ params }: { params: KafkaParams }) {
  const nodeList = (await getNodes(params.kafkaId)).payload;

  const nodes: Node[] = (nodeList?.data ?? []).map((node) => {
    const leaders = node.attributes.broker?.leaderCount;
    const followers = node.attributes.broker?.replicaCount;
    const diskCapacity = node.attributes.storageCapacity ?? undefined;
    const diskUsage = node.attributes.storageUsed ?? undefined;

    return {
      id: node.id,
      nodePool: node.attributes.nodePool ?? "N/A",
      roles: node.attributes.roles ?? [ "broker" ],
      isLeader: node.attributes.metadataState?.status == "leader",
      brokerStatus: node.attributes.broker ? {
        stable: node.attributes.broker.status === "Running",
        description: node.attributes.broker.status,
      } : undefined,
      controllerStatus: node.attributes.controller ? {
        stable: node.attributes.controller.status !== "QuorumFollowerLagged",
        description: node.attributes.controller.status,
      } : undefined,
      hostname: node.attributes.host ?? undefined,
      rack: node.attributes.rack ?? undefined,
      followers,
      leaders,
      diskCapacity,
      diskUsage,
      kafkaVersion: node.attributes.kafkaVersion ?? undefined,
    };
  });

  const data = Object.fromEntries(
    nodes.map((n) => {
      return [n.id, { followers: n.followers, leaders: n.leaders }];
    }),
  );

  return (
    <PageSection isFilled>
      <DistributionChart data={data} />
      <NodesTable nodes={nodes} />
    </PageSection>
  );
}
