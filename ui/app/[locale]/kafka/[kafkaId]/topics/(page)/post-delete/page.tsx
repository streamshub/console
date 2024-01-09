import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { RedirectOnLoad } from "@/components/RedirectOnLoad";
import { revalidatePath } from "next/cache";

export default function PostDeletePage({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  revalidatePath(`/kafka/${kafkaId}/topics`, "page");
  return <RedirectOnLoad url={`/kafka/${kafkaId}/topics`} />;
}
