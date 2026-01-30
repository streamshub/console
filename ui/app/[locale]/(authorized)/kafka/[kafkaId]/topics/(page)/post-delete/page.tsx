import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";

export default async function PostDeletePage(
  props: {
    params: Promise<KafkaParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId
  } = params;

  return <RedirectOnLoad url={`/kafka/${kafkaId}/topics`} />;
}
