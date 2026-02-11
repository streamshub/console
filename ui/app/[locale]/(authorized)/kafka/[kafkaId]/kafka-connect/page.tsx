import { getTranslations } from "next-intl/server";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { stringToInt } from "@/utils/stringToInt";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectorsTableColumn } from "./ConnectorsTable";
import { getKafkaConnectors } from "@/api/kafkaConnect/action";
import {
  Connectors,
  ConnectorsResponse,
  EnrichedConnector,
} from "@/api/kafkaConnect/schema";
import { ConnectedConnectorsTable } from "./ConnectedConnectorsTable";

function enrichConnectorsData(
  connectors: Connectors[],
  included: ConnectorsResponse["included"],
): EnrichedConnector[] {
  const clusterMap = new Map((included ?? []).map((item) => [item.id, item]));

  return connectors.map((connector) => {
    const connectClusterId = connector.relationships?.connectCluster?.data?.id;
    const cluster = connectClusterId ? clusterMap.get(connectClusterId) : null;

    return {
      ...connector,
      connectClusterId: connectClusterId ?? null,
      connectClusterName: cluster?.attributes?.name ?? null,
      replicas: cluster?.attributes?.replicas ?? null,
    };
  });
}

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connectors_title")} | ${t("common.title")}`,
  };
}

export default async function ConnectorsPage({
  params: paramsPromise,
  searchParams: searchParamsPromise,
}: {
  params: Promise<KafkaParams>;
  searchParams: Promise<{
    kafkaClusters: string | undefined;
    name: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  }>;
}) {
  const params = await paramsPromise;
  const searchParams = await searchParamsPromise;

  const kafkaId = searchParams["kafkaClusters"] || params.kafkaId;
  const name = searchParams["name"];
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as ConnectorsTableColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];

  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={
          <ConnectedConnectorsTable
            connectors={undefined}
            name={name}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
            connectorsCount={0}
            kafkaId={kafkaId}
          />
        }
      >
        <AsyncConnectorsTable
          sort={sort}
          name={name}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          kafkaId={kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncConnectorsTable({
  kafkaId,
  name,
  sortDir,
  sort,
  pageCursor,
  pageSize,
}: {
  sort: ConnectorsTableColumn;
  name: string | undefined;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
} & KafkaParams) {
  const response = await getKafkaConnectors({
    kafkaId,
    name,
    sort: sort,
    sortDir,
    pageSize,
    pageCursor,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors!} />;
  }

  const connectors = response.payload!;

  const enrichedConnectors = enrichConnectorsData(
    connectors.data,
    connectors.included,
  );

  const nextPageCursor = connectors.links.next
    ? `after:${new URLSearchParams(connectors.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = connectors.links.prev
    ? `before:${new URLSearchParams(connectors.links.prev).get("page[before]")}`
    : undefined;

  return (
    <ConnectedConnectorsTable
      sort={sort}
      sortDir={sortDir}
      page={connectors.meta.page.pageNumber || 0}
      perPage={pageSize}
      name={name}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
      connectors={enrichedConnectors}
      connectorsCount={connectors.meta.page.total || 0}
      kafkaId={kafkaId}
    />
  );
}
