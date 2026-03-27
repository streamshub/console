import { GroupParams } from "./Group.params";
import { redirect } from "@/i18n/routing";

export default async function GroupPage({
  params: paramsPromise,
}: {
  params: Promise<GroupParams>;
}) {
  const { kafkaId, groupId } = await paramsPromise;
  redirect(`/kafka/${kafkaId}/groups/${groupId}/members`);
}
