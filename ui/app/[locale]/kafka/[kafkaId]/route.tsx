import { getResource } from "@/api/resources";
import { setSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { NextRequest } from "next/server";

export async function GET(
  request: NextRequest,
  route: { params: { kafkaId: string } },
) {
  const cluster = await getResource(route.params.kafkaId, "kafka");
  await setSession("kafka", {
    lastUsed: cluster,
  });
  redirect(`${route.params.kafkaId}/overview`);
}
