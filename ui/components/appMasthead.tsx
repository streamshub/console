import {
  Masthead,
  MastheadContent,
  MastheadMain,
} from "@/libs/patternfly/react-core";
import logo from "@/public/strimzi-dark.png";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import { ReactNode } from "react";

export function AppMasthead({
  main,
  content,
}: {
  main?: ReactNode;
  content?: ReactNode;
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
        {main}
      </MastheadMain>
      <MastheadContent>{content}</MastheadContent>
    </Masthead>
  );
}
