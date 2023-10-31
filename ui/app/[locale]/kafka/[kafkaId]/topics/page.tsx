import { getTopics } from "@/api/topics";
import {
  SortableColumns,
  SortableTopicsTableColumns,
  TopicsTable,
} from "@/app/[locale]/kafka/[kafkaId]/topics/TopicsTable";
import { PageSection } from "@/libs/patternfly/react-core";
import { stringToInt } from "@/utils/stringToInt";

const sortMap: Record<(typeof SortableColumns)[number], string> = {
  name: "name",
  messages: "recordCount",
  partitions: "partitions",
  storage: "totalLeaderLogBytes",
};

export default async function TopicsPage({
  params,
  searchParams,
}: {
  params: {
    kafkaId: string;
  };
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  const pageSize = stringToInt(searchParams.perPage) || 10;
  const sort = (searchParams["sort"] || "name") as SortableTopicsTableColumns;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const topics = await getTopics(params.kafkaId, {
    sort: sortMap[sort],
    sortDir,
    pageSize,
    pageCursor,
  });

  return (
    <PageSection isFilled>
      <TopicsTable
        topics={topics.data}
        topicsCount={topics.meta.page.total}
        perPage={pageSize}
        sort={sort}
        sortDir={sortDir}
        canCreate={process.env.CONSOLE_MODE === "read-write"}
        baseurl={`/kafka/${params.kafkaId}/topics`}
      />
    </PageSection>
  );
}
