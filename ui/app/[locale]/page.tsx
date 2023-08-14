import { getPrincipals } from "@/api/getPrincipals";
import { AppMasthead } from "@/components/appMasthead";
import { PrincipalCard } from "@/components/principalCard";
import { Search } from "@/components/search";
import {
  Button,
  Gallery,
  Page,
  PageSection,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default async function Home() {
  const principals = await getPrincipals();
  return <HomeContent principals={principals} />;
}

function HomeContent({ principals }: { principals: Principal[] }) {
  const t = useTranslations();

  return (
    <Page header={<AppMasthead content={<Search />} />}>
      <PageSection variant={"light"}>
        <TextContent>
          <Title headingLevel={"h1"}>
            Select a Principal to access a Strimzi Cluster
          </Title>
          <Text>
            Lorem ipsum dolor sit amet, consectetur adipisicing elit. Aspernatur
            fugiat nisi odio repellat similique. Accusamus amet consectetur
            culpa, deleniti dicta dolores ea enim in numquam obcaecati
            provident, voluptatibus. Culpa, earum!
          </Text>
          <Button variant="secondary">Add a new Principal</Button>
        </TextContent>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {principals.map((principal) => (
            <PrincipalCard key={principal.id} {...principal} />
          ))}
        </Gallery>
      </PageSection>
    </Page>
  );
}
