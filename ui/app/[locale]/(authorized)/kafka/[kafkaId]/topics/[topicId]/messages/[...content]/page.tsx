import { getSchema } from "@/api/schema/action";
import { ConnectedSchema } from "./ConnectedSchema";

export default async function ConnectedSchemaPage({
  params,
}: {
  params: { content: string[] };
}) {
  const fullPath = params.content.join("/");
  const content = await getSchema(fullPath);
  const { namespace, name } = content;
  const schemaName = `${namespace}.${name}`;
  return (
    <ConnectedSchema
      content={JSON.stringify(content, null, 2)}
      name={schemaName}
    />
  );
}
