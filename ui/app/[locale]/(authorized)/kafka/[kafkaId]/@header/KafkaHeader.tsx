import { getKafkaCluster } from "@/api/kafka/actions";
import { getTopics } from "@/api/topics/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { Skeleton } from "@patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export const fetchCache = "force-cache";

export function KafkaHeader({ params: { kafkaId } }: { params: KafkaParams }) {
  return (
    <Suspense fallback={<AppHeader title={<Skeleton width="35%" />} />}>
      <ConnectedKafkaHeader params={{ kafkaId }} />
    </Suspense>
  );
}

async function ConnectedKafkaHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const topics = await getTopics(kafkaId, { pageSize: 1 });
  return <AppHeader title={cluster.attributes.name} />;
}
