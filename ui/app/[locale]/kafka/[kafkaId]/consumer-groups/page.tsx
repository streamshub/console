import { getConsumerGroups } from "@/api/consumerGroups/actions";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ConsumerGroupsTable } from "./ConsumerGroupsTable";

export default function ConsumerGroupsPage({
  params: { kafkaId },
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
  return (
    <PageSection>
      <Suspense
        fallback={
          <ConsumerGroupsTable
            kafkaId={kafkaId}
            page={1}
            consumerGroups={undefined}
            refresh={undefined}
            total={0}
          />
        }
      >
        <ConnectedConsumerGroupsTable
          params={{ kafkaId }}
          searchParams={searchParams}
        />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedConsumerGroupsTable({
  params: { kafkaId },
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
  async function refresh() {
    "use server";
    const res = await getConsumerGroups(kafkaId, searchParams);
    return res.data;
  }

  const consumerGroups = await getConsumerGroups(kafkaId, searchParams);
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
