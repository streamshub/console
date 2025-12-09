"use client";

import {
  Button,
  ClipboardCopyButton,
  CodeBlock,
  CodeBlockAction,
  CodeBlockCode,
} from "@/libs/patternfly/react-core";
import { DownloadIcon } from "@/libs/patternfly/react-icons";
import { useState } from "react";

export function SchemaValue({
  schema,
  name,
}: {
  schema: string;
  name: string;
}) {
  const [copyStatus, setCopyStatus] = useState<string>("Copy schema");

  const copyToClipboard = () => {
    navigator.clipboard
      .writeText(schema)
      .then(() => {
        setCopyStatus("Successfully copied");
        setTimeout(() => setCopyStatus("Copy schema"), 2000);
      })
      .catch((error) => {
        console.error("Error copying text to clipboard:", error);
      });
  };

  const onClickDownload = () => {
    const blob = new Blob([schema], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${name}.text`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const actions = (
    <CodeBlockAction>
      <ClipboardCopyButton onClick={copyToClipboard} id={"copy-clipboard"}>
        {copyStatus}
      </ClipboardCopyButton>
      <Button
        variant="link"
        onClick={onClickDownload}
        icon={<DownloadIcon />}
      />
    </CodeBlockAction>
  );
  return (
    <CodeBlock actions={actions}>
      <CodeBlockCode>{schema}</CodeBlockCode>
    </CodeBlock>
  );
}
