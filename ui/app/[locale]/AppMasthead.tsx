import {
  Button,
  Masthead,
  MastheadContent,
  MastheadMain,
  MastheadToggle,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import {
  BarsIcon,
  CogIcon,
  QuestionCircleIcon,
} from "@/libs/patternfly/react-icons";
import logo from "@/public/logo.png";
import { useSession } from "next-auth/react";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import { UserDropdown } from "./UserDropdown";

export function AppMasthead({
  onToggleSidebar,
}: {
  onToggleSidebar: () => void;
}) {
  const t = useTranslations();
  const { data } = useSession();
  const { user } = data || {};

  return (
    <Masthead>
      <MastheadToggle>
        <PageToggleButton
          variant="plain"
          aria-label="Global navigation"
          onClick={onToggleSidebar}
        >
          <BarsIcon />
        </PageToggleButton>
      </MastheadToggle>
      <MastheadMain>
        <Link href={"/"} className={"pf-v5-c-masthead_brand"}>
          <Image
            className={"pf-v5-c-brand"}
            src={logo}
            alt={`Strimzi ${t("common.title")}`}
            priority={true}
            unoptimized={true}
          />
        </Link>
      </MastheadMain>
      <MastheadContent>
        <Toolbar
          ouiaId="masthead-toolbar"
          id={"masthead-toolbar"}
          isFullHeight
          isStatic
        >
          <ToolbarContent id={"masthead-toolbar"}>
            <ToolbarGroup
              variant="icon-button-group"
              align={{ default: "alignRight" }}
              spacer={{ default: "spacerNone", md: "spacerMd" }}
            >
              <ToolbarGroup
                variant="icon-button-group"
                visibility={{ default: "hidden", lg: "visible" }}
              >
                <ToolbarItem>
                  <Button
                    aria-label="Settings"
                    variant={"plain"}
                    icon={<CogIcon />}
                    ouiaId={"setting-button"}
                  />
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    aria-label="Help"
                    variant={"plain"}
                    icon={<QuestionCircleIcon />}
                    ouiaId={"help-button"}
                  />
                </ToolbarItem>
              </ToolbarGroup>
            </ToolbarGroup>
            <UserDropdown
              username={user?.name || user?.email}
              picture={user?.picture}
            />
          </ToolbarContent>
        </Toolbar>
      </MastheadContent>
    </Masthead>
  );
}
