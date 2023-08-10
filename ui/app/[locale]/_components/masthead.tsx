import { Tool } from "@/app/[locale]/_api/getTools";
import { Toolbar, ToolbarItem } from "@/libs/patternfly/react-core";
import { ApplicationLauncher } from "./applicationLauncher";
import {
  Masthead as PFMasthead,
  MastheadContent,
  MastheadMain,
  Title,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import Image from "next/image";
import Link from "next/link";
import logo from "@/public/strimzi-dark-picto.png";

export function Masthead({ tools }: { tools: Tool[] }) {
  const t = useTranslations();

  return (
    <PFMasthead>
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
        <Toolbar id={"masthead-toolbar"} ouiaId={"masthead-toolbar"}>
          <ToolbarItem align={{ default: "alignRight" }}>
            <ApplicationLauncher tools={tools} />
          </ToolbarItem>
        </Toolbar>
      </MastheadContent>
    </PFMasthead>
  );
}
