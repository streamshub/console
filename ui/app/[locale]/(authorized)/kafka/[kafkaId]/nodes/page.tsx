import { getNodes } from "@/api/nodes/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { DistributionChart } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/DistributionChart";
import { Grid, GridItem, PageSection } from "@/libs/patternfly/react-core";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";
import { NodeListColumn } from "./NodesTable";
import { BrokerStatus, ControllerStatus, NodeRoles } from "@/api/nodes/schema";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectedNodesTable } from "./ConnectedNodesTable";
import { stringToInt } from "@/utils/stringToInt";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("nodes.title")} | ${t("common.title")}`,
  };
}

export default function NodesPage({
  params,
  searchParams,
}: {
  params: KafkaParams;
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
    nodePool: string | undefined;
    roles: string | undefined;
    status: string | undefined;
  };
}) {
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as NodeListColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const nodePool = (searchParams["nodePool"] || "")
    .split(",")
    .filter((v) => !!v) as string[] | undefined;
  const roles = (searchParams["roles"] || "").split(",").filter((v) => !!v) as
    | NodeRoles[]
    | undefined;
  const status = (searchParams["status"] || "")
    .split(",")
    .filter((v) => !!v) as (BrokerStatus | ControllerStatus)[] | undefined;

  return (
    <Suspense
      fallback={
        <ConnectedNodesTable
          nodes={undefined}
          nodesCount={0}
          page={1}
          perPage={pageSize}
          nodePool={nodePool}
          sort={sort}
          status={status}
          sortDir={sortDir}
          roles={roles}
          nextPageCursor={undefined}
          prevPageCursor={undefined}
          nodePoolList={undefined}
        />
      }
    >
      <AsyncNodesTable
        sort={sort}
        sortDir={sortDir}
        pageSize={pageSize}
        pageCursor={pageCursor}
        nodePool={nodePool}
        roles={roles}
        kafkaId={params.kafkaId}
        status={status}
      />
    </Suspense>
  );
}

async function AsyncNodesTable({
  kafkaId,
  sortDir,
  sort,
  pageCursor,
  pageSize,
  nodePool,
  status,
  roles,
}: {
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  nodePool: string[] | undefined;
  roles: NodeRoles[] | undefined;
  status: (BrokerStatus | ControllerStatus)[] | undefined;
} & KafkaParams) {
  const nodeCounts = {
    totalNodes: 0,
    totalBrokers: 0,
    totalControllers: 0,
    leadControllerId: "",
  };

  const response = await getNodes(kafkaId, {
    sort: sort,
    sortDir,
    pageSize,
    pageCursor,
    roles,
    status,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const nodes = response.payload!;

  const nextPageCursor = nodes.links.next
    ? `after:${new URLSearchParams(nodes.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = nodes.links.prev
    ? `before:${new URLSearchParams(nodes.links.prev).get("page[before]")}`
    : undefined;

  const data = Object.fromEntries(
    nodes.data
      .filter((n) => n.attributes.roles.includes("broker"))
      .map((n) => {
        return [
          n.id,
          {
            followers: n.attributes.broker?.replicaCount,
            leaders: n.attributes.broker?.leaderCount,
          },
        ];
      }),
  );

  nodes.data.forEach((node) => {
    nodeCounts.totalNodes++;

    if (node.attributes.roles.includes("broker")) {
      nodeCounts.totalBrokers++;
    }

    if (node.attributes.roles.includes("controller")) {
      nodeCounts.totalControllers++;
    }

    if (node.attributes.metadataState?.status === "leader") {
      nodeCounts.leadControllerId = node.id;
    }
  });

  return (
    <PageSection isFilled>
      <Grid hasGutter>
        <GridItem>
          <DistributionChart data={data} nodesCount={nodeCounts} />
        </GridItem>
        <GridItem>
          <ConnectedNodesTable
            nodes={nodes.data}
            nodesCount={nodes.meta.page.total}
            nodePoolList={nodes.meta.summary.nodePools}
            page={nodes.meta.page.pageNumber || 1}
            perPage={pageSize}
            nodePool={nodePool}
            sort={sort}
            sortDir={sortDir}
            roles={roles}
            status={status}
            nextPageCursor={nextPageCursor}
            prevPageCursor={prevPageCursor}
          />
        </GridItem>
      </Grid>
    </PageSection>
  );
}
