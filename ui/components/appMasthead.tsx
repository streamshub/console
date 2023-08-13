import {
  Masthead,
  MastheadContent,
  MastheadMain,
  Title,
} from "@/libs/patternfly/react-core";
import logo from "@/public/strimzi-dark-picto.png";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import { PropsWithChildren } from "react";

export function AppMasthead({ children }: PropsWithChildren<{}>) {
  const t = useTranslations();

  return (
    <Masthead id="stack-masthead" display={{ default: "stack" }}>
      <MastheadMain>
        <Link href={"/"} className={"pf-v5-u-mx-xl"}>
          <Image
            src={logo}
            alt={`Strimzi ${t("common.title")}`}
            style={{ height: 36, width: "auto" }}
            priority={true}
          />
        </Link>
        <Title headingLevel={"h1"} size="2xl" ouiaId={"masthead-title"}>
          {t("homepage.title")}
        </Title>
      </MastheadMain>
      <MastheadContent>{children}</MastheadContent>
    </Masthead>
  );
}
