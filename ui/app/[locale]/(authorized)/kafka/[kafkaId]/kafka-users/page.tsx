import { getKafkaUsers } from "@/api/kafkaUsers/action";
import { KafkaParams } from "../kafka.params";
import { KafkaUserColumn } from "./KafkaUsersTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { ConnectedKafkaUsersTable } from "./ConnectedKafkaUsersTable";
import { stringToInt } from "@/utils/stringToInt";
import { Suspense } from "react";
import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `${t("kafkausers.title")} | ${t("common.title")}`,
  };
}

export default async function KafkaUsersPage({
  params: paramsPromise,
  searchParams: searchParamsPromise,
}: {
  params: Promise<KafkaParams>;
  searchParams: Promise<{
    username: string | undefined;
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  }>;
}) {
  const { kafkaId } = await paramsPromise;
  const searchParams = await searchParamsPromise;

  const pageSize = stringToInt(searchParams.perPage) || 20;
  const sort = (searchParams["sort"] || "name") as KafkaUserColumn;
  const sortDir = (searchParams["sortDir"] || "asc") as "asc" | "desc";
  const pageCursor = searchParams["page"];
  const username = searchParams["username"];

  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={
          <ConnectedKafkaUsersTable
            kafkaUsers={undefined}
            kafkaUserCount={0}
            page={1}
            perPage={pageSize}
            username={undefined}
            sortDir={sortDir}
            sort={sort}
            nextPageCursor={undefined}
            prevPageCursor={undefined}
            kafkaId={""}
          />
        }
      >
        <AsyncKafkaUserTable
          sort={sort}
          sortDir={sortDir}
          pageSize={pageSize}
          pageCursor={pageCursor}
          username={username}
          kafkaId={kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncKafkaUserTable({
  kafkaId,
  sort,
  sortDir,
  pageCursor,
  pageSize,
  username,
}: {
  sort: KafkaUserColumn;
  sortDir: "asc" | "desc";
  pageSize: number;
  pageCursor: string | undefined;
  username: string | undefined;
} & KafkaParams) {
  const response = await getKafkaUsers(kafkaId, {
    sort: sort,
    sortDir,
    pageSize,
    pageCursor,
    username,
  });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const kafkaUsers = response.payload!;

  const nextPageCursor = kafkaUsers.links.next
    ? `after:${new URLSearchParams(kafkaUsers.links.next).get("page[after]")}`
    : undefined;

  const prevPageCursor = kafkaUsers.links.prev
    ? `before:${new URLSearchParams(kafkaUsers.links.prev).get("page[before]")}`
    : undefined;

  return (
    <ConnectedKafkaUsersTable
      kafkaId={kafkaId}
      kafkaUsers={kafkaUsers.data}
      kafkaUserCount={kafkaUsers.meta.page.total || 0}
      page={kafkaUsers.meta.page.pageNumber || 0}
      perPage={pageSize}
      username={username}
      sortDir={sortDir}
      sort={sort}
      nextPageCursor={nextPageCursor}
      prevPageCursor={prevPageCursor}
    />
  );
}
