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

export default async function NodesPage(
  props: {
    params: Promise<KafkaParams>;
    searchParams: Promise<{
      perPage: string | undefined;
      sort: string | undefined;
      sortDir: string | undefined;
      page: string | undefined;
      nodePool: string | undefined;
      roles: string | undefined;
      brokerStatus: string | undefined;
      controllerStatus: string | undefined;
    }>;
  }
) {
  const searchParams = await props.searchParams;
  const params = await props.params;
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
  const brokerStatus = (searchParams["brokerStatus"] || "")
    .split(",")
    .filter((v) => !!v) as BrokerStatus[] | undefined;
  const controllerStatus = (searchParams["controllerStatus"] || "")
    .split(",")
    .filter((v) => !!v) as ControllerStatus[] | undefined;

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
          brokerStatus={brokerStatus}
          sortDir={sortDir}
          roles={roles}
          nextPageCursor={undefined}
          prevPageCursor={undefined}
          nodePoolList={undefined}
          controllerStatus={controllerStatus}
          statuses={undefined}
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
        brokerStatus={brokerStatus}
        controllerStatus={controllerStatus}
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
  brokerStatus,
  controllerStatus,
  roles,
}: {
  sort: NodeListColumn;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  nodePool: string[] | undefined;
  roles: NodeRoles[] | undefined;
  brokerStatus: BrokerStatus[] | undefined;
  controllerStatus: ControllerStatus[] | undefined;
} & KafkaParams) {
  const nodeCounts = {
    totalNodes: 0,
    brokers: {
        total: 0,
        warning: false
    },
    controllers: {
        total: 0,
        warning: false
    },
    leadControllerId: "",
  };

  const response = await getNodes(kafkaId, {
    sort: sort,
    sortDir,
    pageSize,
    pageCursor,
    roles,
    brokerStatus,
    controllerStatus,
    nodePool,
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
      .filter((n) => n.attributes.roles?.includes("broker"))
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

  nodeCounts.totalNodes = Object.values(
    nodes.meta.summary.statuses.combined ?? 0,
  ).reduce((tally, value) => tally + value, 0);

  nodeCounts.brokers.total = Object.values(
    nodes.meta.summary.statuses.brokers ?? 0,
  ).reduce((tally, value) => tally + value, 0);
  // Set warning if any are not running
  nodeCounts.brokers.warning = Object.keys(
    nodes.meta.summary.statuses.brokers ?? {}
  ).some(key => key !== "Running");

  nodeCounts.controllers.total = Object.values(
    nodes.meta.summary.statuses.controllers ?? 0,
  ).reduce((tally, value) => tally + value, 0);
  // Set warning if any are not running leader or follower (e.g. lagged or unknown)
  nodeCounts.controllers.warning = Object.keys(
    nodes.meta.summary.statuses.controllers ?? {}
  ).some(key => key !== "QuorumLeader" && key !== "QuorumFollower");

  nodeCounts.leadControllerId = nodes.meta.summary.leaderId ?? "";

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
            brokerStatus={brokerStatus}
            controllerStatus={controllerStatus}
            nextPageCursor={nextPageCursor}
            prevPageCursor={prevPageCursor}
            statuses={nodes.meta.summary.statuses}
          />
        </GridItem>
      </Grid>
    </PageSection>
  );
}
