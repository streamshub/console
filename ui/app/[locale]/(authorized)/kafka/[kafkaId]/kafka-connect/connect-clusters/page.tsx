import { getTranslations } from "next-intl/server";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { stringToInt } from "@/utils/stringToInt";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectedConnectClustersTable } from "./ConnectedConnectClustersTable";
import { ConnectClustersTableColumn } from "./ConnectClustersTable";
import { getKafkaConnectClusters } from "@/api/kafkaConnect/action";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("KafkaConnect.connect_clusters_title")} | ${t("common.title")}`,
  };
}

export default async function ConnectClustersPage(
  props: {
    params: Promise<KafkaParams>;
    searchParams: Promise<{
      kafkaClusters: string | undefined;
      name: string | undefined;
      perPage: string | undefined;
      sort: string | undefined;
      sortDir: string | undefined;
      page: string | undefined;
    }>;
  }
) {
  const searchParams = await props.searchParams;
  const params = await props.params;
  const kafkaId = searchParams["kafkaClusters"] || params.kafkaId;
  const name = searchParams["name"];
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as ConnectClustersTableColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={
          <ConnectedConnectClustersTable
            kafkaId={kafkaId}
            connectClusters={undefined}
            name={name}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
            connectClustersCount={0}
          />
        }
      >
        <AsyncConnectClustersTable
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

async function AsyncConnectClustersTable({
  kafkaId,
  name,
  sortDir,
  sort,
  pageCursor,
  pageSize,
}: {
  sort: ConnectClustersTableColumn;
  name: string | undefined;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
} & KafkaParams) {
  const response = await getKafkaConnectClusters({
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

  const connectClusters = response.payload!;

  const nextPageCursor = connectClusters.links.next
    ? `after:${new URLSearchParams(connectClusters.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = connectClusters.links.prev
    ? `before:${new URLSearchParams(connectClusters.links.prev).get("page[before]")}`
    : undefined;

  return (
    <ConnectedConnectClustersTable
      sort={sort}
      sortDir={sortDir}
      page={connectClusters.meta.page.pageNumber || 0}
      perPage={pageSize}
      name={name}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
      connectClusters={connectClusters.data}
      connectClustersCount={connectClusters.meta.page.total || 0}
      kafkaId={kafkaId}
    />
  );
}
