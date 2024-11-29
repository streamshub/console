"use server";

import { getHeaders } from "../api";

export async function getSchema(contentLink: string) {
  const url = `${process.env.BACKEND_URL}/${contentLink}`;
  const res = await fetch(url, {
    headers: await getHeaders(),
  });
  const rawData = await res.json();
  return rawData;
}
