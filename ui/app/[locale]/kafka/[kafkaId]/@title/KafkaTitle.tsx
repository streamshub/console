import { getKafkaCluster } from "@/api/kafka";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { Title } from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";

export async function KafkaTitle({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }

  return <Title headingLevel={"h1"}>{cluster.attributes.name}</Title>;
}
