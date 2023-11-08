import { getKafkaCluster } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { Title } from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { notFound } from "next/navigation";
import { Suspense } from "react";

export const fetchCache = "force-cache";

export function KafkaTitle({ params: { kafkaId } }: { params: KafkaParams }) {
  return (
    <Title headingLevel={"h1"}>
      <Suspense fallback={<Skeleton width="35%" />}>
        <ConnectedKafkaTitle params={{ kafkaId }} />
      </Suspense>
    </Title>
  );
}

async function ConnectedKafkaTitle({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }

  return cluster.attributes.name;
}
