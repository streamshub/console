import { getTranslations } from "next-intl/server";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { stringToInt } from "@/utils/stringToInt";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectedConnectorsTable } from "./ConnectedConnectorsTable";
import { ConnectorsTableColumn } from "./ConnectorsTable";
import { getKafkaConnectors } from "@/api/kafkaConnect/action";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connectors_title")} | ${t("common.title")}`,
  };
}

export default function ConnectorsPage({
  searchParams,
}: {
  searchParams: {
    kafkaId: string;
    name: string | undefined;
    consumerGroupState: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  const kafkaId = searchParams["connectCluster.kafkaClusters"];
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
      connectors={connectors.data}
      connectorsCount={connectors.meta.page.total || 0}
    />
  );
}
