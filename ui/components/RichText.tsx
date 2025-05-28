import React, { ReactNode } from "react";
import { Content } from "@/libs/patternfly/react-core";

type CommonRichTextTag = "strong" | "b" | "i" | "br" | "p" | "text";

type RichTextProps = {
  children: (
    tags: Record<CommonRichTextTag, (chunks: ReactNode) => ReactNode>,
  ) => ReactNode;
};

export default function RichText({ children }: RichTextProps) {
  const tagsMap: Record<CommonRichTextTag, (chunks: ReactNode) => ReactNode> = {
    strong: (chunks) => <strong>{chunks}</strong>,
    b: (chunks) => <b>{chunks}</b>,
    i: (chunks) => <i>{chunks}</i>,
    br: () => <br />,
    p: (chunks) => <p>{chunks}</p>,
    text: (chunks) => <Content>{chunks}</Content>,
  };

  return <span className="next-intl-rich-text">{children(tagsMap)}</span>;
}
