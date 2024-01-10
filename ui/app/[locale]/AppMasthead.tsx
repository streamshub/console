"use client";
import { useAppLayout } from "@/app/[locale]/AppLayoutProvider";
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
import logo from "@/public/Logo-Red_Hat-AMQ-A-Reverse-RGB.svg";
import { FeedbackModal } from "@patternfly/react-user-feedback";
import { useSession } from "next-auth/react";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { UserDropdown } from "./UserDropdown";

export function AppMasthead() {
  const t = useTranslations();
  const { data } = useSession();
  const { user } = data || {};
  const { toggleSidebar } = useAppLayout();
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
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label="Global navigation"
            onClick={toggleSidebar}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <Link href={"/"} className={"pf-v5-c-masthead_brand"}>
            <Image
              className={"pf-v5-c-brand"}
              src={logo}
              alt={t("common.title")}
              priority={true}
              style={{ height: 48 }}
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
                      onClick={openFeedbackModal}
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
      <FeedbackModal
        onShareFeedback="https://github.com/eyefloaters/console/issues/new?assignees=&labels=&projects=&template=share-feeedback.md&title="
        onJoinMailingList="https://github.com/eyefloaters/console/issues/new?assignees=&labels=&projects=&template=inform-the-direction-of-products.md&title="
        onOpenSupportCase="https://github.com/eyefloaters/console/issues/new?assignees=&labels=&projects=&template=open-support-case.md&title="
        onReportABug="https://github.com/eyefloaters/console/issues/new?assignees=&labels=&projects=&template=bug_report.md&title="
        feedbackImg={"/pf_feedback.svg"}
        isOpen={isFeedbackModalOpen}
        onClose={closeFeedbackModal}
      />
    </>
  );
}
