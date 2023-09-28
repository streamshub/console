import { getResource } from "@/api/resources";
import { setSession } from "@/utils/session";
import { NextRequest, NextResponse } from "next/server";

export async function GET(
  _request: NextRequest,
  route: { params: { kafkaId: string } },
) {
  const cluster = await getResource(route.params.kafkaId, "kafka");
  if (cluster?.attributes.cluster) {
    await setSession("kafka", {
      lastUsed: cluster,
    });
  }
  return NextResponse.next();
}
