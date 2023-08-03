import {
  Brand,
  Button,
  ButtonVariant,
  Divider,
  DropdownGroup,
  DropdownItem,
  Masthead,
  MastheadBrand,
  MastheadMain,
  Page,
  PageBreadcrumb,
  PageGroup,
  SkipToContent,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@patternfly/react-core";
import { ThIcon } from "@patternfly/react-icons";
import BellIcon from "@patternfly/react-icons/dist/esm/icons/bell-icon";
import CogIcon from "@patternfly/react-icons/dist/esm/icons/cog-icon";
import HelpIcon from "@patternfly/react-icons/dist/esm/icons/help-icon";
import QuestionCircleIcon from "@patternfly/react-icons/dist/esm/icons/question-circle-icon";
import { useTranslations } from "next-intl";
import Head from "next/head";
import React, { type FormEvent, type PropsWithChildren, useState } from "react";

interface NavOnSelectProps {
  groupId: number | string;
  itemId: number | string;
  to: string;
  event: FormEvent<HTMLInputElement>;
}

export default function Layout({
  breadcrumb,
  title,
  children,
}: PropsWithChildren<{
  breadcrumb?: JSX.Element;
  title?: string;
}>): JSX.Element {
  const t = useTranslations("common");

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isKebabDropdownOpen, setIsKebabDropdownOpen] = useState(false);
  const [isFullKebabDropdownOpen, setIsFullKebabDropdownOpen] = useState(false);
  const [isAppLauncherOpen, setIsAppLauncherOpen] = useState(false);
  const [activeItem, setActiveItem] = useState(1);

  const onNavSelect = (selectedItem: NavOnSelectProps) => {
    typeof selectedItem.itemId === "number" &&
      setActiveItem(selectedItem.itemId);
  };

  const onDropdownToggle = (isOpen: boolean) => {
    setIsDropdownOpen(isOpen);
  };

  const onDropdownSelect = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  const onKebabDropdownToggle = (isOpen: boolean) => {
    setIsKebabDropdownOpen(isOpen);
  };

  const onKebabDropdownSelect = () => {
    setIsKebabDropdownOpen(!isKebabDropdownOpen);
  };

  const onFullKebabDropdownToggle = (isOpen: boolean) => {
    setIsFullKebabDropdownOpen(isOpen);
  };

  const onFullKebabDropdownSelect = () => {
    setIsFullKebabDropdownOpen(!isFullKebabDropdownOpen);
  };

  const onAppLauncherToggle = (isOpen: boolean) => {
    setIsAppLauncherOpen(isOpen);
  };

  const onAppLauncherSelect = () => {
    setIsAppLauncherOpen(!isAppLauncherOpen);
  };

  const kebabDropdownItems = [
    <DropdownItem key="settings">
      <CogIcon /> Settings
    </DropdownItem>,
    <DropdownItem key="help">
      <HelpIcon /> Help
    </DropdownItem>,
  ];

  const fullKebabDropdownItems = [
    <DropdownGroup key="group 2">
      <DropdownItem key="group 2 profile">My profile</DropdownItem>
      <DropdownItem key="group 2 user" component="button">
        User management
      </DropdownItem>
      <DropdownItem key="group 2 logout">Logout</DropdownItem>
    </DropdownGroup>,
    <Divider key="divider" />,
    <DropdownItem key="settings">
      <CogIcon /> Settings
    </DropdownItem>,
    <DropdownItem key="help">
      <HelpIcon /> Help
    </DropdownItem>,
  ];

  const headerToolbar = (
    <Toolbar id="toolbar" isFullHeight isStatic>
      <ToolbarContent>
        <ToolbarGroup
          variant="icon-button-group"
          spacer={{ default: "spacerNone", md: "spacerMd" }}
        >
          <ToolbarItem>
            <Button
              aria-label="Notifications"
              variant={ButtonVariant.plain}
              icon={<BellIcon />}
            />
          </ToolbarItem>
          <ToolbarGroup
            variant="icon-button-group"
            visibility={{ default: "hidden", lg: "visible" }}
          >
            <ToolbarItem
              visibility={{ default: "hidden", md: "hidden", lg: "visible" }}
            >
              <ThIcon />
            </ToolbarItem>
            <ToolbarItem>
              <Button
                aria-label="Settings"
                variant={ButtonVariant.plain}
                icon={<CogIcon />}
              />
            </ToolbarItem>
            <ToolbarItem>
              <Button
                aria-label="Help"
                variant={ButtonVariant.plain}
                icon={<QuestionCircleIcon />}
              />
            </ToolbarItem>
          </ToolbarGroup>
          <ToolbarItem
            visibility={{ default: "hidden", md: "visible", lg: "hidden" }}
          ></ToolbarItem>
          <ToolbarItem visibility={{ md: "hidden" }}></ToolbarItem>
        </ToolbarGroup>
      </ToolbarContent>
    </Toolbar>
  );

  const masthead = (
    <Masthead>
      <MastheadMain>
        <MastheadBrand>
          <Brand
            src={
              "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d8/Red_Hat_logo.svg/2560px-Red_Hat_logo.svg.png"
            }
            alt={t("title")}
            heights={{ default: "24px" }}
          />
          &nbsp;&nbsp;{t("title")}
        </MastheadBrand>
      </MastheadMain>
    </Masthead>
  );

  const mainContainerId = "main-content";

  const pageSkipToContent = (
    <SkipToContent href={`#${mainContainerId}`}>Skip to content</SkipToContent>
  );

  return (
    <>
      <Head>
        <title>
          {title && `${title} - `}
          {t("title")}
        </title>
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <Page
        header={masthead}
        skipToContent={pageSkipToContent}
        mainContainerId={mainContainerId}
        isBreadcrumbWidthLimited
        isBreadcrumbGrouped
        additionalGroupedContent={
          // <PageSection variant={PageSectionVariants.light} isWidthLimited>
          //   <TextContent>
          //     <Text component="h1">Main title</Text>
          //     <Text component="p">This is a full page demo.</Text>
          //   </TextContent>
          // </PageSection>
          undefined
        }
        groupProps={{
          stickyOnBreakpoint: { default: "top" },
        }}
      >
        <PageGroup>
          {breadcrumb && <PageBreadcrumb>{breadcrumb}</PageBreadcrumb>}
        </PageGroup>
        {children}
      </Page>
    </>
  );
}
