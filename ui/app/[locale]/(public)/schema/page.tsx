import { getSchema } from "@/api/schema/action";
import { ConnectedSchema } from "./ConnectedSchema";

export default async function ConnectedSchemaPage({
  searchParams: searchParamsPromise,
}: {
  searchParams: Promise<{ content?: string; schemaname?: string }>;
}) {
  const searchParams = await searchParamsPromise;
  const urlSearchParams = new URLSearchParams(searchParams);

  const content = urlSearchParams.get("content");
  const schemaname = urlSearchParams.get("schemaname");

  if (!content) {
    throw new Error("Content parameter is missing.");
  }

  const schemaContent = await getSchema(content);

  return <ConnectedSchema content={schemaContent} name={schemaname || ""} />;
}
