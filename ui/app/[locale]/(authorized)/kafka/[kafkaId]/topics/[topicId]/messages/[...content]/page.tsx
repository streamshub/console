import { getSchema } from "@/api/schema/action";
import { ConnectedSchema } from "./ConnectedSchema";

export default async function ConnectedSchemaPage({
  params,
}: {
  params: { content: string[] };
}) {
  const fullPath = params.content.join("/");
  const content = await getSchema(fullPath);

  return <ConnectedSchema content={content} name={""} />;
}
