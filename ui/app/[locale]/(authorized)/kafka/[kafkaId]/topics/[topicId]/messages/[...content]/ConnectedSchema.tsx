"use client";

import { SchemaValue } from "@/components/MessagesTable/components/SchemaValue";
import { PageSection, Title } from "@patternfly/react-core";

export function ConnectedSchema({
  content,
  name,
}: {
  content: string;
  name: string;
}) {
  return (
    <PageSection variant="light">
      <Title headingLevel={"h4"}>{name}</Title>
      <SchemaValue schema={content} name={name} />
    </PageSection>
  );
}
