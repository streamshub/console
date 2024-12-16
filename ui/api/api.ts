import { getUser } from "@/utils/session";
import { z } from "zod";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "api" });

const SERVER_ROOT = !process.env.BACKEND_URL?.endsWith("/") ?
    process.env.BACKEND_URL :
    process.env.BACKEND_URL.substring(0, process.env.BACKEND_URL.length - 1);

export function sortParam(
    sortField: string | undefined,
    order: string | undefined
) {
  if (sortField) {
    return (order === "asc" ? "-" : "") + sortField;
  }
  return undefined;
}

export function filterEq(value: string | undefined) {
  if (value) {
    return `eq,${value}`;
  }
  return undefined;
}

export function filterGte(value: string | undefined) {
  if (value) {
    return `gte,${value}`;
  }
  return undefined;
}

export function filterLike(value: string | undefined) {
  if (value) {
    return `like,*${value}*`;
  }
  return undefined;
}

export function filterIn(values: string[] | undefined) {
  if (values?.length ?? -1 > 0) {
    return `in,${values?.join(",")}`;
  }
  return undefined;
}

export async function getHeaders(anonymous?: boolean): Promise<Record<string, string>> {
  const user = anonymous? null : await getUser();
  let headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (user?.authorization) {
    headers["Authorization"] = user.authorization;
  }
  return headers;
}

export async function fetchData<T>(
    path: string,
    query: URLSearchParams | string,
    parser: (json: any) => T,
    anonymous?: boolean,
    options?: { cache?: "no-store" | "force-cache", next?: { revalidate: false | 0 | number } }
) : Promise<ApiResponse<T>> {

  const queryString = query?.toString() ?? "";
  const url = `${SERVER_ROOT}${path}${queryString.length > 0 ? "?" + queryString : ""}`;

  const response = await fetch(
    url, {
      headers: await getHeaders(anonymous),
      ...options
    });
  const rawData = await response.json();

  if (response.ok) {
    log.debug(rawData, `fetch ${url} response`);

    return {
      payload: parser(rawData),
      timestamp: new Date(),
    };
  } else {
    log.info(rawData, `fetch ${url} response`);

    return {
      errors: ApiErrorResponse.parse(rawData).errors,
      timestamp: new Date(),
    };
  }
}

export async function postData<T>(
    path: string,
    body: any,
    parser: (json: any) => T,
) : Promise<ApiResponse<T>> {

  const url = `${SERVER_ROOT}${path}`;

  const response = await fetch(
    url, {
      method: 'POST',
      headers: await getHeaders(),
      body: JSON.stringify(body),
    });

  const rawData = await response.text();

  if (response.ok) {
    log.debug(rawData, `patch ${url} response`);

    return {
      payload: rawData.length > 0 ? parser(JSON.parse(rawData)) : null,
      timestamp: new Date(),
    };
  } else {
    log.info(rawData, `patch ${url} response`);

    return {
      errors: ApiErrorResponse.parse(JSON.parse(rawData)).errors,
      timestamp: new Date(),
    };
  }
}

export async function patchData<T>(
    path: string,
    body: any,
    parser: (json: any) => T,
) : Promise<ApiResponse<T>> {

  const url = `${SERVER_ROOT}${path}`;

  const response = await fetch(
    url, {
      method: 'PATCH',
      headers: await getHeaders(),
      body: JSON.stringify(body),
    });

  const rawData = await response.text();

  if (response.ok) {
    log.debug(rawData, `patch ${url} response`);

    return {
      payload: rawData.length > 0 ? parser(JSON.parse(rawData)) : null,
      timestamp: new Date(),
    };
  } else {
    log.info(rawData, `patch ${url} response`);

    return {
      errors: ApiErrorResponse.parse(JSON.parse(rawData)).errors,
      timestamp: new Date(),
    };
  }
}

export const ApiErrorSchema = z.object({
  meta: z.object({ type: z.string() }).optional(), // z.map(z.string(), z.string()),
  id: z.string().optional(),
  status: z.string().optional(),
  code: z.string().optional(),
  title: z.string(),
  detail: z.string(),
  source: z
    .object({
      pointer: z.string().optional(),
      parameter: z.string().optional(),
      header: z.string().optional(),
    })
    .optional(),
});

export type ApiError = z.infer<typeof ApiErrorSchema>;

export const ApiErrorResponse = z.object({
  meta: z.object({}).nullable().optional(),
  errors: z.array(ApiErrorSchema),
});

export type ApiResponse<T> = {
    errors?: ApiError[];
    payload?: T | null;
    timestamp: Date;
};
