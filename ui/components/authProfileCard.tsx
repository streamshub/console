"use client";
import { AuthProfile } from "@/api/auth";
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

export function AuthProfileCard({
  id,
  attributes: { name, cluster, mechanism },
}: AuthProfile) {
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
        <CardTitle>
          {name}@{cluster.attributes.name}
        </CardTitle>
      </CardHeader>
      <CardBody>
        <DescriptionList>
          <DescriptionListGroup>
            <DescriptionListTerm>Cluster Address</DescriptionListTerm>
            <DescriptionListDescription>
              <Truncate
                content={cluster.attributes.bootstrapServers}
                position={"middle"}
              />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Cluster ID</DescriptionListTerm>
            <DescriptionListDescription>
              {cluster.attributes.name}
            </DescriptionListDescription>
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
