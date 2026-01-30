import { getTranslations } from "next-intl/server";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { RebalanceTableColumn, RebalanceTableColumns } from "./RebalanceTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { stringToInt } from "@/utils/stringToInt";
import { Suspense } from "react";
import { ConnectedReabalancesTable } from "./ConnectedRebalancesTable";
import { getRebalancesList } from "@/api/rebalance/actions";
import { RebalanceMode, RebalanceStatus } from "@/api/rebalance/schema";
import { NoDataErrorState } from "@/components/NoDataErrorState";

//export const dynamic = "force-dynamic";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("Rebalancing.title")} | ${t("common.title")}`,
  };
}

const sortMap: Record<(typeof RebalanceTableColumns)[number], string> = {
  name: "name",
  status: "status",
  lastUpdated: "lastUpdated",
};

export default async function RebalancesPage(
  props: {
    params: Promise<KafkaParams>;
    searchParams: Promise<{
      id: string | undefined;
      name: string | undefined;
      perPage: string | undefined;
      sort: string | undefined;
      sortDir: string | undefined;
      page: string | undefined;
      status: string | undefined;
      mode: string | undefined;
    }>;
  }
) {
  const searchParams = await props.searchParams;
  const params = await props.params;
  const name = searchParams["name"];
  const mode = (searchParams["mode"] || "").split(",").filter((v) => !!v) as
    | RebalanceMode[]
    | undefined;
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as RebalanceTableColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const status = (searchParams["status"] || "")
    .split(",")
    .filter((v) => !!v) as RebalanceStatus[] | undefined;

  return (
    <PageSection isFilled>
      <Suspense
        fallback={
          <ConnectedReabalancesTable
            rebalances={undefined}
            rebalancesCount={0}
            name={name}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            status={status}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
            mode={mode}
            baseurl={`/kafka/${params.kafkaId}/nodes`}
            kafkaId={params.kafkaId}
          />
        }
      >
        <AsyncReabalanceTable
          name={name}
          sort={sort}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          kafkaId={params.kafkaId}
          status={status}
          mode={mode}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncReabalanceTable({
  kafkaId,
  name,
  sortDir,
  sort,
  pageCursor,
  pageSize,
  status,
  mode,
}: {
  sort: RebalanceTableColumn;
  name: string | undefined;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  status: RebalanceStatus[] | undefined;
  mode: RebalanceMode[] | undefined;
} & KafkaParams) {
  const response = await getRebalancesList(kafkaId, {
    name,
    sort: sortMap[sort],
    sortDir,
    pageSize,
    pageCursor,
    status,
    mode,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors!} />;
  }

  const rebalance = response.payload!;

  const nextPageQuery = rebalance.links.next
    ? new URLSearchParams(rebalance.links.next)
    : undefined;
  const nextPageCursor = nextPageQuery?.get("page[after]");
  const prevPageQuery = rebalance.links.prev
    ? new URLSearchParams(rebalance.links.prev)
    : undefined;
  const prevPageCursor = prevPageQuery?.get("page[after]");
  return (
    <ConnectedReabalancesTable
      name={name}
      perPage={pageSize}
      sort={sort}
      sortDir={sortDir}
      status={status}
      baseurl={`/kafka/${kafkaId}/nodes`}
      page={rebalance.meta.page.pageNumber || 1}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
      rebalances={rebalance.data}
      rebalancesCount={rebalance.data.length}
      mode={mode}
      kafkaId={kafkaId}
    />
  );
}
