"use client";
import { ClusterList } from "@/api/kafka/schema";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import { Link } from "@/navigation";
import {
  LoginMainFooterBandItem,
  LoginPage,
  Menu,
  MenuItem,
} from "@patternfly/react-core";
import { useTranslations } from "next-intl";

export function ClusterSelector({ clusters }: { clusters: ClusterList[] }) {
  const t = useTranslations();
  const productName = t("common.product");

  const learnMoreResource = (
    <LoginMainFooterBandItem>
      <ExternalLink
        href={"https://redhat.com"}
        testId={"learn-more-about-streams-kafka"}
      >
        {t("login-in-page.learning_resource", { product: productName })}
      </ExternalLink>
    </LoginMainFooterBandItem>
  );

  return (
    <LoginPage
      backgroundImgSrc="/assets/images/pfbg-icon.svg"
      loginTitle={t("homepage.page_header", { product: productName })}
      loginSubtitle={t("cluster-selector.subtitle")}
      textContent={t("login-in-page.text_content", { product: productName })}
      brandImgSrc={"/StreamsLogo.svg"}
      socialMediaLoginContent={learnMoreResource}
    >
      <Menu isPlain={true}>
        {clusters.map((c) => (
          <MenuItem
            key={c.id}
            component={(props) => (
              <Link href={`/kafka/${c.id}/login`}>{c.attributes.name}</Link>
            )}
          />
        ))}
      </Menu>
    </LoginPage>
  );
}
