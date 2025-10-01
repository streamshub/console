"use server";

import { fetchData, ApiResponse } from "@/api/api";
import { MetadataResponse, MetadataSchema } from "./schema";

export async function getMetadata(): Promise<ApiResponse<MetadataResponse>> {
  return fetchData(
    `/api/metadata`,
    "",
    (rawData) => MetadataSchema.parse(rawData.data),
    true,
    {
        next: {
            // 60s cache
            revalidate: 60,
        }
    },
  );
}
