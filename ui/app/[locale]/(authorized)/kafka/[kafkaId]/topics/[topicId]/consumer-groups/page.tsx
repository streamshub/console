import { getTranslations } from "next-intl/server";
import { getTopicConsumerGroups } from "@/api/consumerGroups/actions";
import { ConsumerGroupsTable } from "./ConsumerGroupsTable";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `Topic Consumer Groups | ${t("common.title")}`,
  };
}

export default async function ConsumerGroupsPage(
  props: {
    params: Promise<KafkaTopicParams>;
    searchParams: Promise<{
      perPage: string | undefined;
      sort: string | undefined;
      sortDir: string | undefined;
      page: string | undefined;
    }>;
  }
) {
  const searchParams = await props.searchParams;
  const params = await props.params;

  const {
    kafkaId,
    topicId
  } = params;

  return (
    <PageSection>
      <Suspense
        fallback={
          <ConsumerGroupsTable
            kafkaId={kafkaId}
            page={1}
            total={0}
          />
        }
      >
        <ConnectedConsumerGroupsPage
          params={{ kafkaId, topicId }}
          searchParams={searchParams}
        />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedConsumerGroupsPage({
  params: { kafkaId, topicId },
  searchParams,
}: {
  params: KafkaTopicParams;
  searchParams: {
    perPage: string | undefined;
    sort: string | undefined;
    sortDir: string | undefined;
    page: string | undefined;
  };
}) {
  async function refresh() {
    "use server";
    const res = await getTopicConsumerGroups(kafkaId, topicId, searchParams);
    return res.payload?.data ?? null;
  }

  const response = await getTopicConsumerGroups(
    kafkaId,
    topicId,
    searchParams,
  );

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const consumerGroups = response.payload!;

  return (
    <ConsumerGroupsTable
      kafkaId={kafkaId}
      page={consumerGroups.meta.page.pageNumber || 1}
      total={consumerGroups.meta.page.total || 0}
      consumerGroups={consumerGroups.data}
      refresh={refresh}
    />
  );
}
