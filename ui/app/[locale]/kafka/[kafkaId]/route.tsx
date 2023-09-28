import { getResource } from "@/api/resources";
import { setSession } from "@/utils/session";
import { NextRequest, NextResponse } from "next/server";

export async function GET(
  _request: NextRequest,
  context: { params: { kafkaId: string } },
) {
  const cluster = await getResource(context.params.kafkaId, "kafka");
  await setSession("kafka", {
    lastUsed: cluster,
  });
  return NextResponse.redirect(`${context.params.kafkaId}/overview`);
}
