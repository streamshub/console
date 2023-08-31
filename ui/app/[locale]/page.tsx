import { AuthProfile, getAuthProfiles } from "@/api/auth";
import { AppMasthead } from "@/components/appMasthead";
import { AuthProfileCard } from "@/components/authProfileCard";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Gallery,
  Page,
  PageSection,
  Split,
  SplitItem,
  TextContent,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";
import { useTranslations } from "next-intl";

export default async function Home() {
  const user = await getUser();
  const authProfiles = await getAuthProfiles();
  return <HomeContent username={user.username} authProfiles={authProfiles} />;
}

function HomeContent({
  username,
  authProfiles,
}: {
  username: string;
  authProfiles: AuthProfile[];
}) {
  const t = useTranslations();

  return (
    <Page
      header={
        <AppMasthead
          username={username}
          content={
            <Toolbar id="auth-profile-toolbar">
              <ToolbarContent>
                <ToolbarItem>
                  <Breadcrumb ouiaId="main-breadcrumb">
                    <BreadcrumbItem isActive>
                      Authorization Profiles
                    </BreadcrumbItem>
                  </Breadcrumb>
                </ToolbarItem>
              </ToolbarContent>
            </Toolbar>
          }
        />
      }
    >
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">
                Select an Authorization Profile to access a Cluster
              </Title>
            </TextContent>
          </SplitItem>
          <SplitItem>
            <Button>Add a new Authorization Profile</Button>
          </SplitItem>
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {authProfiles.map((ap) => (
            <AuthProfileCard key={ap.id} {...ap} />
          ))}
        </Gallery>
      </PageSection>
    </Page>
  );
}
