import { getSchema } from "@/api/schema/action";
import { SchemaValue } from "@/components/MessagesTable/components/SchemaValue";

export default async function ContentPage({ params }: { params: { content: string } }) {

  const contentData = await getSchema(params.content);
  return (
    <SchemaValue schema={JSON.stringify(contentData, null, 2) || ''} name={""}/>
  );
  
}
