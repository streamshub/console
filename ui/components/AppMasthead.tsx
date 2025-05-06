"use client";
import { TechPreviewPopover } from "@/components/TechPreviewPopover";
import {
  Brand,
  Button,
  Label,
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
  MoonIcon,
  QuestionCircleIcon,
  SunIcon,
} from "@/libs/patternfly/react-icons";
import { FeedbackModal } from "@patternfly/react-user-feedback";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { useAppLayout } from "./AppLayoutProvider";
import { UserDropdown } from "./UserDropdown";
import { useDarkMode } from "@/app/[locale]/useDarkMode";

export function AppMasthead({
  username,
  showSidebarToggle,
}: {
  username?: string;
  showSidebarToggle: boolean;
}) {
  const t = useTranslations();
  const { toggleSidebar } = useAppLayout();
  const { isDarkMode, toggleDarkMode } = useDarkMode();

  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);

  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };
  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
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
                    : "full_logo_hori_default.svg"
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
    </>
  );
}
