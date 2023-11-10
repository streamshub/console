import { getTopics } from "@/api/topics/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import {
  SortableColumns,
  SortableTopicsTableColumns,
  TopicsTable,
} from "@/app/[locale]/kafka/[kafkaId]/topics/(page)/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { stringToInt } from "@/utils/stringToInt";
import { Suspense } from "react";

const sortMap: Record<(typeof SortableColumns)[number], string> = {
  name: "name",
  messages: "recordCount",
  partitions: "partitions",
  storage: "totalLeaderLogBytes",
};

export default function TopicsPage({
  params,
  searchParams,
}: {
  params: KafkaParams;
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as SortableTopicsTableColumns;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];

  return (
    <PageSection isFilled>
      <Suspense
        fallback={
          <TopicsTable
            topics={undefined}
            topicsCount={0}
            perPage={pageSize}
            sort={sort}
            sortDir={sortDir}
            canCreate={process.env.CONSOLE_MODE === "read-write"}
            baseurl={`/kafka/${params.kafkaId}/topics`}
            page={1}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
          />
        }
      >
        <ConnectedTopicsTable
          sort={sort}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          kafkaId={params.kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedTopicsTable({
  kafkaId,
  sortDir,
  sort,
  pageCursor,
  pageSize,
}: {
  sort: SortableTopicsTableColumns;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
} & KafkaParams) {
  const topics = await getTopics(kafkaId, {
    sort: sortMap[sort],
    sortDir,
    pageSize,
    pageCursor,
  });

  const nextPageQuery = topics.links.next
    ? new URLSearchParams(topics.links.next)
    : undefined;
  const nextPageCursor = nextPageQuery?.get("page[after]");
  const prevPageQuery = topics.links.prev
    ? new URLSearchParams(topics.links.prev)
    : undefined;
  const prevPageCursor = prevPageQuery?.get("page[after]");
  return (
    <TopicsTable
      topics={topics.data}
      topicsCount={topics.meta.page.total}
      perPage={pageSize}
      sort={sort}
      sortDir={sortDir}
      canCreate={process.env.CONSOLE_MODE === "read-write"}
      baseurl={`/kafka/${kafkaId}/topics`}
      page={topics.meta.page.pageNumber}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
    />
  );
}
