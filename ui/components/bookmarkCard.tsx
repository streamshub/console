"use client";
import { Bookmark } from "@/api/types";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  Truncate,
} from "@/libs/patternfly/react-core";
import { CardFooter } from "@patternfly/react-core";
import Link from "next/link";
import { useRouter } from "next/navigation";

export function BookmarkCard({
  id,
  attributes: { name, mechanism, principal, bootstrapServer },
}: Bookmark) {
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
      <CardHeader>
        <CardTitle>{name}</CardTitle>
      </CardHeader>
      <CardBody>
        <DescriptionList>
          <DescriptionListGroup>
            <DescriptionListTerm>Cluster Address</DescriptionListTerm>
            <DescriptionListDescription>
              <Truncate content={bootstrapServer} position={"middle"} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Principal</DescriptionListTerm>
            <DescriptionListDescription>{principal}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>SASL Mechanism</DescriptionListTerm>
            <DescriptionListDescription>{mechanism}</DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </CardBody>
      <Divider />
      <CardFooter>
        <Link href={`/${id}`}>Connect to this cluster</Link>
      </CardFooter>
    </Card>
  );
}
