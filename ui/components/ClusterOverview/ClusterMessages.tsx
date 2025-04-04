// ClusterMessages.tsx
"use client";

import { ClusterDetail } from "@/api/kafka/schema";
import { useEffect, useState } from "react";

type ClusterMessagesProps = {
  cluster: ClusterDetail | null;
};

type Message = {
  variant: "danger" | "warning";
  subject: { type: string; name: string; id: string };
  message: string;
  date: string;
};

export function ClusterMessages({ cluster }: ClusterMessagesProps) {
  const [messages, setMessages] = useState<Message[]>([]);

  useEffect(() => {
    const fetchMessages = () => {
      if (!cluster?.attributes.conditions) {
        setMessages([]);
        return;
      }
      const newMessages = cluster.attributes.conditions
        .filter((c) => "Ready" !== c.type)
        .map((c) => ({
          variant:
            c.type === "Error" ? "danger" : ("warning" as "danger" | "warning"),
          subject: {
            type: c.type!,
            name: cluster?.attributes.name ?? "",
            id: cluster?.id ?? "",
          },
          message: c.message ?? "",
          date: c.lastTransitionTime ?? "",
        }));
      setMessages(newMessages);
    };

    fetchMessages();

    const intervalId = setInterval(fetchMessages, 5000);

    return () => clearInterval(intervalId);
  }, [cluster?.attributes.conditions, cluster?.attributes.name, cluster?.id]);

  if (!cluster?.attributes.conditions) return [];

  return cluster.attributes.conditions
    .filter((c) => c.type !== "Ready")
    .map((c) => ({
      variant: c.type === "Error" ? "danger" : "warning",
      subject: {
        type: c.type!,
        name: cluster.attributes.name ?? "",
        id: cluster.id ?? "",
      },
      message: c.message ?? "",
      date: c.lastTransitionTime ?? "",
    }));
}
