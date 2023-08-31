import {
  Button,
  Masthead,
  MastheadContent,
  MastheadMain,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import logo from "@/public/strimzi-dark.png";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import { ReactNode } from "react";
import { UserDropdown } from "./userDropdown";
import { CogIcon, QuestionCircleIcon } from "@/libs/patternfly/react-icons";

export function AppMasthead({ username, content }: {
  username: string;
  content: ReactNode;
}) {
  const t = useTranslations();

  return (
    <Masthead id="stack-masthead" display={{ default: "stack" }}>
      <MastheadMain>
        <Link href={"/"} className={"pf-v5-c-masthead_brand pf-v5-u-mx-xl"}>
          <Image
            className={"pf-v5-c-brand"}
            src={logo}
            alt={`Strimzi ${t("common.title")}`}
            priority={true}
            unoptimized={true}
          />
        </Link>
        <Toolbar id="toolbar" isFullHeight isStatic>
          <ToolbarContent>
            <ToolbarGroup
              variant="icon-button-group"
              align={{ default: 'alignRight' }}
              spacer={{ default: 'spacerNone', md: 'spacerMd' }}
            >
              <ToolbarGroup variant="icon-button-group" visibility={{ default: 'hidden', lg: 'visible' }}>
                <ToolbarItem>
                  <Button aria-label="Settings" variant={'plain'} icon={<CogIcon />} />
                </ToolbarItem>
                <ToolbarItem>
                  <Button aria-label="Help" variant={'plain'} icon={<QuestionCircleIcon />} />
                </ToolbarItem>
              </ToolbarGroup>
              <UserDropdown username={username} />
            </ToolbarGroup>
          </ToolbarContent>
        </Toolbar>
      </MastheadMain>
      <MastheadContent>
        {content}
      </MastheadContent>
    </Masthead>
  );
}
