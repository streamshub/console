import { getTranslations } from "next-intl/server";
import { getTopicConsumerGroups } from "@/api/consumerGroups/actions";
import { ConsumerGroupsTable } from "./ConsumerGroupsTable";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `Topic Consumer Groups | ${t("common.title")}`,
  };
}

export default function ConsumerGroupsPage({
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
  return (
    <PageSection>
      <Suspense
        fallback={
          <ConsumerGroupsTable
            kafkaId={kafkaId}
            page={1}
            total={0}
            consumerGroups={undefined}
            refresh={undefined}
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
    return res.data;
  }

  const consumerGroups = await getTopicConsumerGroups(
    kafkaId,
    topicId,
    searchParams,
  );
  if (!consumerGroups) {
    notFound();
  }
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
