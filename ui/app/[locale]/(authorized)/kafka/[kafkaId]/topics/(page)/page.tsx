import { getTranslations } from "next-intl/server";
import { getTopics } from "@/api/topics/actions";
import { TopicStatus } from "@/api/topics/schema";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import {
  SortableColumns,
  SortableTopicsTableColumns,
} from "@/components/TopicsTable/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { stringToInt } from "@/utils/stringToInt";
import { Suspense } from "react";
import { ConnectedTopicsTable } from "./ConnectedTopicsTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("topics.title")} | ${t("common.title")}`,
  };
}

const sortMap: Record<(typeof SortableColumns)[number], string> = {
  name: "name",
  partitions: "partitions",
  storage: "totalLeaderLogBytes",
};

export default async function TopicsPage({
  params: paramsPromise,
  searchParams: searchParamsPromise,
}: {
  params: Promise<KafkaParams>;
  searchParams: Promise<{
    id: string | undefined;
    name: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
    includeHidden: string | undefined;
    status: string | undefined;
  }>;
}) {
  const params = await paramsPromise;
  const searchParams = await searchParamsPromise;

  const id = searchParams["id"];
  const name = searchParams["name"];
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as SortableTopicsTableColumns;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const includeHidden = searchParams["includeHidden"] === "true";
  const status = (searchParams["status"] || "")
    .split(",")
    .filter((v) => !!v) as TopicStatus[] | undefined;

  return (
    <PageSection isFilled>
      <Suspense
        fallback={
          <ConnectedTopicsTable
            topics={undefined}
            topicsCount={0}
            id={id}
            name={name}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            includeHidden={includeHidden}
            status={status}
            baseurl={`/kafka/${params.kafkaId}/topics`}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
          />
        }
      >
        <AsyncTopicsTable
          id={id}
          name={name}
          sort={sort}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          kafkaId={params.kafkaId}
          includeHidden={includeHidden}
          status={status}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncTopicsTable({
  kafkaId,
  id,
  name,
  sortDir,
  sort,
  pageCursor,
  pageSize,
  includeHidden,
  status,
}: {
  sort: SortableTopicsTableColumns;
  id: string | undefined;
  name: string | undefined;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  includeHidden: boolean;
  status: TopicStatus[] | undefined;
} & KafkaParams) {
  const response = await getTopics(kafkaId, {
    id,
    name,
    sort: sortMap[sort],
    sortDir,
    pageSize,
    pageCursor,
    includeHidden,
    status,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const topics = response.payload!;
  const nextPageCursor = topics.links.next
    ? `after:${new URLSearchParams(topics.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = topics.links.prev
    ? `before:${new URLSearchParams(topics.links.prev).get("page[before]")}`
    : undefined;

  return (
    <ConnectedTopicsTable
      topics={topics}
      topicsCount={topics.meta.page.total}
      id={id}
      name={name}
      perPage={pageSize}
      sort={sort}
      sortDir={sortDir}
      includeHidden={includeHidden}
      status={status}
      baseurl={`/kafka/${kafkaId}/topics`}
      page={topics.meta.page.pageNumber || 1}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
    />
  );
}
