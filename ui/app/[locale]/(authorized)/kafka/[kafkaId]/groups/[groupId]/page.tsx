import { GroupParams } from "./Group.params";
import { redirect } from "@/i18n/routing";

export default function GroupPage({
  params: { kafkaId, groupId },
}: {
  params: GroupParams;
}) {
  redirect(`/kafka/${kafkaId}/groups/${groupId}/members`);
}
