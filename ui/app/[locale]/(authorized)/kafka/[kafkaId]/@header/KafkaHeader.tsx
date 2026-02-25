import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { Skeleton } from "@/libs/patternfly/react-core";
import { Suspense } from "react";

export async function KafkaHeader({
  params: paramsPromise,
}: {
  params: Promise<KafkaParams>;
}) {
  const { kafkaId } = await paramsPromise;
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
  const cluster = (await getKafkaCluster(kafkaId))?.payload;

  if (cluster) {
    return <AppHeader title={cluster.attributes.name} />;
  }

  return <AppHeader title={"-"} />;
}
