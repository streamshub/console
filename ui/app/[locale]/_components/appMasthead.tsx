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
import { ReactNode } from "react";
import { Tool } from "../_api/getTools";
import { ApplicationLauncher } from "./applicationLauncher";

export function AppMasthead({
  tools,
  toolbar,
}: {
  tools: Tool[];
  toolbar: ReactNode;
}) {
  const t = useTranslations();

  return (
    <Masthead
      id="stack-masthead"
      display={{ default: "stack" }}
      className={"pf-v5-theme-dark"}
    >
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
      <MastheadContent>
        <ApplicationLauncher tools={tools} />
        {toolbar}
      </MastheadContent>
    </Masthead>
  );
}
