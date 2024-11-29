"use client";

import { SchemaValue } from "@/components/MessagesTable/components/SchemaValue";
import { PageSection } from "@patternfly/react-core";

export function ConnectedSchema({
  content,
  name,
}: {
  content: string;
  name: string;
}) {
  return (
    <PageSection variant="light">
      <SchemaValue schema={JSON.stringify(content, null, 2)} name={name} />
    </PageSection>
  );
}
