import { getTranslations } from "next-intl/server";
import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import {
  SortableColumns,
  SortableConsumerGroupTableColumns,
} from "./ConsumerGroupsTable";
import { ConsumerGroupState } from "@/api/consumerGroups/schema";
import { ConnectedConsumerGroupTable } from "./ConnectedConsumerGroupTable";
import { stringToInt } from "@/utils/stringToInt";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("ConsumerGroups.title")} | ${t("common.title")}`,
  };
}

const sortMap: Record<(typeof SortableColumns)[number], string> = {
  groupId: "groupId",
  state: "state",
};

export default async function ConsumerGroupsPage({
  params: paramsPromise,
  searchParams: searchParamsPromise,
}: {
  params: Promise<KafkaParams>;
  searchParams: Promise<{
    id: string | undefined;
    consumerGroupState: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  }>;
}) {
  const { kafkaId } = await paramsPromise;
  const searchParams = await searchParamsPromise;

  const id = searchParams["id"];
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] ||
    "groupId") as SortableConsumerGroupTableColumns;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const consumerGroupState = (searchParams["consumerGroupState"] || "")
    .split(",")
    .filter((v) => !!v) as ConsumerGroupState[] | undefined;
  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={
          <ConnectedConsumerGroupTable
            kafkaId={kafkaId}
            consumerGroup={undefined}
            id={id}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            consumerGroupState={consumerGroupState}
            baseurl={`/kafka/${kafkaId}/consumer-groups`}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
            consumerGroupCount={0}
          />
        }
      >
        <AsyncConsumerGroupTable
          sort={sort}
          id={id}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          consumerGroupState={consumerGroupState}
          kafkaId={kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncConsumerGroupTable({
  kafkaId,
  id,
  sortDir,
  sort,
  pageCursor,
  pageSize,
  consumerGroupState,
}: {
  sort: SortableConsumerGroupTableColumns;
  id: string | undefined;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  consumerGroupState: ConsumerGroupState[] | undefined;
} & KafkaParams) {
  const response = await getConsumerGroups(kafkaId, {
    id,
    sort: sortMap[sort],
    sortDir,
    pageSize,
    pageCursor,
    consumerGroupState,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors!} />;
  }

  const consumerGroups = response.payload!;

  const nextPageCursor = consumerGroups.links.next
    ? `after:${new URLSearchParams(consumerGroups.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = consumerGroups.links.prev
    ? `before:${new URLSearchParams(consumerGroups.links.prev).get("page[before]")}`
    : undefined;

  return (
    <ConnectedConsumerGroupTable
      kafkaId={kafkaId}
      consumerGroup={consumerGroups.data}
      consumerGroupCount={consumerGroups.meta.page.total || 0}
      sort={sort}
      sortDir={sortDir}
      page={consumerGroups.meta.page.pageNumber || 0}
      perPage={pageSize}
      id={id}
      consumerGroupState={consumerGroupState}
      baseurl={`/kafka/${kafkaId}/consumer-groups`}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
    />
  );
}
