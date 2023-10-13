"use client";
"use client";
import { ClusterList } from "@/api/kafka";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Truncate,
} from "@/libs/patternfly/react-core";
import { useRouter } from "next/navigation";

export function ClusterCard({
  id,
  attributes: { name, bootstrapServers },
}: ClusterList) {
  const router = useRouter();
  const cardId = `tool-card-${id}`;
  return (
    <Card
      isClickable={true}
      key={id}
      id={cardId}
      ouiaId={cardId}
      isCompact={true}
    >
      <CardHeader
        selectableActions={{
          onClickAction: () => router.push(`new-auth-profile/${id}`),
          selectableActionId: id,
          selectableActionAriaLabelledby: cardId,
        }}
      >
        <CardTitle>{name}</CardTitle>
      </CardHeader>
      <CardBody>
        <DescriptionList>
          <DescriptionListGroup>
            <DescriptionListTerm>Cluster Address</DescriptionListTerm>
            <DescriptionListDescription>
              <Truncate content={bootstrapServers} position={"middle"} />
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </CardBody>
    </Card>
  );
}
