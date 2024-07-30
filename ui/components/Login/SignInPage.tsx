"use client";
import { LoginForm, LoginMainFooterBandItem, LoginPage } from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { ExternalLink } from "../Navigation/ExternalLink";

export function SignInPage() {
  const t = useTranslations();
  const productName = t("common.product");
  
  const learnMoreResource = <LoginMainFooterBandItem>
    <ExternalLink
      href={"https://redhat.com"}
      testId={"learn-more-about-streams-kafka"}
    >
      {t("login-in-page.learning_resource", { product: productName})}
    </ExternalLink>
    </LoginMainFooterBandItem>;
    
  return (
    <LoginPage
    backgroundImgSrc="/assets/images/pfbg-icon.svg"
    loginTitle={t("homepage.page_header", { product: productName })}
    loginSubtitle={t("login-in-page.login_sub_title")}
    textContent={t("login-in-page.text_content", {product: productName })}
    brandImgSrc={"/StreamsLogo.svg"}
    footerListItems={t("login-in-page.footer_text")}
    socialMediaLoginContent={learnMoreResource}
    >
      <LoginForm
      usernameLabel={t("login-in-page.username")}
      passwordLabel={t("login-in-page.password")}
      loginButtonLabel={t("login-in-page.login_button")}
      />
    </LoginPage>
  )
}
