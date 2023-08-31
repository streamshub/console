import { AuthProfile, getAuthProfiles } from "@/api/getAuthProfiles";
import { AppMasthead } from "@/components/appMasthead";
import { AuthProfileCard } from "@/components/authProfileCard";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Gallery,
  Page,
  PageSection,
  Text,
  TextContent,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem
} from "@/libs/patternfly/react-core";
import { getUser } from "@/utils/session";
import { useTranslations } from "next-intl";

export default async function Home() {
  const user = await getUser();
  const authProfiles = await getAuthProfiles();
  return <HomeContent username={user.username} authProfiles={authProfiles} />;
}

function HomeContent({ username, authProfiles }: { username: string, authProfiles: AuthProfile[] }) {
  const t = useTranslations();

  return (
    <Page header={<AppMasthead username={username} content={
      <Toolbar id="auth-profile-toolbar">
        <ToolbarContent>
          <ToolbarItem>
            <Breadcrumb ouiaId="BasicBreadcrumb">
              <BreadcrumbItem isActive>
                Authorization Profiles
              </BreadcrumbItem>
            </Breadcrumb>
          </ToolbarItem>
          <ToolbarGroup
            align={{ default: 'alignRight' }}
          >
            <ToolbarItem>
              <Button>Add a new Authorization Profile</Button>
            </ToolbarItem>
          </ToolbarGroup>
        </ToolbarContent>
      </Toolbar>
    } />}>
      <PageSection variant={"light"}>
        <TextContent>
          <Text>Select an Authorization Profile to access a Cluster</Text>
        </TextContent>
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
