import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";
import { revalidatePath } from "next/cache";

export default function PostDeletePage({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  revalidatePath(`/kafka/${kafkaId}/topics`, "page");
  return <RedirectOnLoad url={`/kafka/${kafkaId}/topics`} />;
}
