import { filterUndefinedFromObj } from "@/utils/filterUndefinedFromObj";
import { ApiResponse, fetchData, filterEq, sortParam } from "../api";
import {
  KafkaUser,
  KafkaUserResponseSchema,
  KafkaUsersResponse,
  KafkaUsersResponseSchema,
} from "./schema";

export async function getKafkaUser(
  kafkaId: string,
  userId: string,
): Promise<ApiResponse<KafkaUser>> {
  return fetchData(
    `/api/kafkas/${kafkaId}/users/${userId}`,
    "",
    (rawData) => KafkaUserResponseSchema.parse(rawData).data,
  );
}

export async function getKafkaUsers(
  kafkaId: string,
  params: {
    fields?: string;
    username?: string;
    pageSize?: number;
    pageCursor?: string;
    sort?: string;
    sortDir?: string;
  },
): Promise<ApiResponse<KafkaUsersResponse>> {
  const sp = new URLSearchParams(
    filterUndefinedFromObj({
      "fields[kafkaUsers]":
        params.fields ??
        "name,namespace,creationTimestamp,username,authenticationType,authorization",
      "filter[username]": filterEq(params.username),
      "page[size]": params.pageSize,
      "page[after]": params.pageCursor?.startsWith("after:")
        ? params.pageCursor.slice(6)
        : undefined,
      "page[before]": params.pageCursor?.startsWith("before:")
        ? params.pageCursor.slice(7)
        : undefined,
      sort: sortParam(params.sort, params.sortDir),
    }),
  );
  return fetchData(`/api/kafkas/${kafkaId}/users`, sp, (rawData) =>
    KafkaUsersResponseSchema.parse(rawData),
  );
}
