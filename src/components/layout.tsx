import {
  ApplicationLauncher,
  ApplicationLauncherItem,
  Button,
  ButtonVariant,
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  KebabToggle,
  Masthead,
  MastheadBrand,
  MastheadContent,
  MastheadMain,
  MastheadToggle,
  Page,
  PageSection,
  PageToggleButton,
  SkipToContent,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@patternfly/react-core";
import BarsIcon from "@patternfly/react-icons/dist/esm/icons/bars-icon";
import BellIcon from "@patternfly/react-icons/dist/esm/icons/bell-icon";
import CogIcon from "@patternfly/react-icons/dist/esm/icons/cog-icon";
import HelpIcon from "@patternfly/react-icons/dist/esm/icons/help-icon";
import QuestionCircleIcon from "@patternfly/react-icons/dist/esm/icons/question-circle-icon";
import React, { type FormEvent, type PropsWithChildren, useState } from "react";

interface NavOnSelectProps {
  groupId: number | string;
  itemId: number | string;
  to: string;
  event: FormEvent<HTMLInputElement>;
}

export default function Layout({
  breadcrumb,
  children,
}: PropsWithChildren<{ breadcrumb?: JSX.Element }>): JSX.Element {
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

  const appLauncherItems = [
    <ApplicationLauncherItem key="application_1a" href="#">
      Application 1 (anchor link)
    </ApplicationLauncherItem>,
    <ApplicationLauncherItem
      key="application_2a"
      component="button"
      onClick={() => alert("Clicked item 2")}
    >
      Application 2 (button with onClick)
    </ApplicationLauncherItem>,
  ];

  const headerToolbar = (
    <Toolbar id="toolbar" isFullHeight isStatic>
      <ToolbarContent>
        <ToolbarGroup
          variant="icon-button-group"
          alignment={{ default: "alignRight" }}
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
              <ApplicationLauncher
                onSelect={onAppLauncherSelect}
                onToggle={onAppLauncherToggle}
                isOpen={isAppLauncherOpen}
                items={appLauncherItems}
              />
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
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={onKebabDropdownSelect}
              toggle={<KebabToggle onToggle={onKebabDropdownToggle} />}
              isOpen={isKebabDropdownOpen}
              dropdownItems={kebabDropdownItems}
            />
          </ToolbarItem>
          <ToolbarItem visibility={{ md: "hidden" }}>
            <Dropdown
              isPlain
              position="right"
              onSelect={onFullKebabDropdownSelect}
              toggle={<KebabToggle onToggle={onFullKebabDropdownToggle} />}
              isOpen={isFullKebabDropdownOpen}
              dropdownItems={fullKebabDropdownItems}
            />
          </ToolbarItem>
        </ToolbarGroup>
      </ToolbarContent>
    </Toolbar>
  );

  const masthead = (
    <Masthead>
      <MastheadToggle>
        <PageToggleButton variant="plain" aria-label="Global navigation">
          <BarsIcon />
        </PageToggleButton>
      </MastheadToggle>
      <MastheadMain>
        <MastheadBrand>
          {/*<Brand*/}
          {/*  widths={{ default: "180px", md: "180px", "2xl": "220px" }}*/}
          {/*  src="/assets/images/logo__pf--reverse--base.png"*/}
          {/*  alt="Fallback patternFly default logo"*/}
          {/*>*/}
          {/*  <source*/}
          {/*    media="(min-width: 768px)"*/}
          {/*    srcSet="/assets/images/logo__pf--reverse-on-md.svg"*/}
          {/*  />*/}
          {/*  <source srcSet="/assets/images/logo__pf--reverse--base.svg" />*/}
          {/*</Brand>*/}
          AMQ Streams
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>{headerToolbar}</MastheadContent>
    </Masthead>
  );

  const mainContainerId = "main-content";

  const pageSkipToContent = (
    <SkipToContent href={`#${mainContainerId}`}>Skip to content</SkipToContent>
  );

  return (
    <Page
      header={masthead}
      isManagedSidebar
      skipToContent={pageSkipToContent}
      breadcrumb={breadcrumb}
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
      <PageSection>{children}</PageSection>
    </Page>
  );
}
