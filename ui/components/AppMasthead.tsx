"use client";
import {
  AboutModal,
  Brand,
  Button,
  Content,
  Masthead,
  MastheadBrand,
  MastheadContent,
  MastheadLogo,
  MastheadMain,
  MastheadToggle,
  PageToggleButton,
  ToggleGroup,
  ToggleGroupItem,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import {
  BarsIcon,
  InfoCircleIcon,
  MoonIcon,
  QuestionCircleIcon,
  SunIcon,
} from "@/libs/patternfly/react-icons";
import { FeedbackModal } from "@patternfly/react-user-feedback";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useAppLayout } from "./AppLayoutProvider";
import { UserDropdown } from "./UserDropdown";
import { useDarkMode } from "@/app/[locale]/useDarkMode";
import { AppDropdown, ClusterInfo } from "./AppDropdown";
import { MetadataResponse } from "@/api/meta/schema";

export function AppMasthead({
  username,
  showSidebarToggle,
  clusterInfoList,
  kafkaId,
  metadata,
  isOidcEnabled,
}: {
  readonly username?: string;
  readonly showSidebarToggle: boolean;
  readonly clusterInfoList: ClusterInfo[];
  readonly kafkaId: string;
  readonly metadata?: MetadataResponse;
  readonly isOidcEnabled?: boolean;
}) {
  const t = useTranslations();
  const { toggleSidebar } = useAppLayout();
  const { isDarkMode, toggleDarkMode } = useDarkMode();

  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);
  const [isAboutModalOpen, setIsAboutModalOpen] = useState(false);

  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };
  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
  };
  const toggleAboutModal = (
    _event: React.MouseEvent<Element, MouseEvent> | KeyboardEvent | MouseEvent,
  ) => {
    setIsAboutModalOpen(!isAboutModalOpen);
  };

  return (
    <>
      <Masthead>
        <MastheadMain>
          {showSidebarToggle && (
            <MastheadToggle>
              <PageToggleButton
                variant="plain"
                aria-label={t("AppMasthead.global_navigation")}
                onClick={toggleSidebar}
              >
                <BarsIcon />
              </PageToggleButton>
            </MastheadToggle>
          )}
          <MastheadBrand>
            <MastheadLogo href="/" target="_blank">
              <Brand
                src={
                  isDarkMode
                    ? "/full_logo_hori_reverse.svg"
                    : "/full_logo_hori_default.svg"
                }
                alt={t("common.title")}
                heights={{ default: "56px" }}
              />
            </MastheadLogo>
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>
          <Toolbar
            ouiaId="masthead-toolbar"
            id={"masthead-toolbar"}
            isFullHeight
            isStatic
          >
            <ToolbarContent id={"masthead-toolbar"}>
              {showSidebarToggle && (
                <ToolbarItem className={"pf-v6-u-py-sm"}>
                  <AppDropdown
                    clusters={clusterInfoList}
                    kafkaId={kafkaId}
                    isOidcEnabled={isOidcEnabled}
                  />
                </ToolbarItem>
              )}
              <ToolbarGroup
                variant="action-group"
                align={{ default: "alignEnd" }}
              >
                <ToggleGroup className={"pf-v6-u-py-sm"}>
                  <ToggleGroupItem
                    icon={<SunIcon />}
                    aria-label="Light mode"
                    isSelected={!isDarkMode}
                    onChange={() => {
                      toggleDarkMode(false);
                    }}
                  />
                  <ToggleGroupItem
                    icon={<MoonIcon />}
                    aria-label="Dark mode"
                    isSelected={isDarkMode}
                    onChange={() => {
                      toggleDarkMode(true);
                    }}
                  />
                </ToggleGroup>
                <ToolbarGroup
                  variant="label-group"
                  visibility={{ default: "hidden", lg: "visible" }}
                >
                  <ToolbarItem className={"pf-v6-u-py-sm"}>
                    <Button
                      aria-label={t("AppMasthead.help")}
                      variant={"plain"}
                      icon={<QuestionCircleIcon />}
                      ouiaId={"help-button"}
                      onClick={openFeedbackModal}
                    />
                  </ToolbarItem>
                  {metadata && (
                    <ToolbarItem className={"pf-v6-u-py-sm"}>
                      <Button
                        aria-label={t("AppMasthead.help")}
                        variant={"plain"}
                        icon={<InfoCircleIcon />}
                        ouiaId={"help-button"}
                        onClick={toggleAboutModal}
                      />
                    </ToolbarItem>
                  )}
                </ToolbarGroup>
              </ToolbarGroup>
              {username && (
                <UserDropdown username={username} picture={undefined} />
              )}
            </ToolbarContent>
          </Toolbar>
        </MastheadContent>
      </Masthead>

      <FeedbackModal
        onShareFeedback={t("feedback.links.share")}
        onJoinMailingList={t("feedback.links.informDirection")}
        onOpenSupportCase={t("feedback.links.supportCase")}
        onReportABug={t("feedback.links.bugReport")}
        feedbackImg={"/pf_feedback.svg"}
        isOpen={isFeedbackModalOpen}
        onClose={closeFeedbackModal}
      />

      <AboutModal
        isOpen={isAboutModalOpen}
        onClose={(
          e: React.MouseEvent<Element, MouseEvent> | KeyboardEvent | MouseEvent,
        ) => toggleAboutModal(e)}
        brandImageSrc={
          isDarkMode
            ? "/full_logo_hori_reverse.svg"
            : "/full_logo_hori_default.svg"
        }
        brandImageAlt="Brand Logo"
        productName={t("common.title")}
      >
        <Content>
          <dl>
            <dt>Version</dt>
            <dd>{metadata?.attributes.version}</dd>
            <dt>Platform</dt>
            <dd>{metadata?.attributes.platform}</dd>
            <dt>User agent</dt>
            <dd>{navigator.userAgent}</dd>
          </dl>
        </Content>
      </AboutModal>
    </>
  );
}
