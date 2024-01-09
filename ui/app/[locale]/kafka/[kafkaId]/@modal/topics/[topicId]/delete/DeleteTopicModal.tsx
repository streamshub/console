"use client";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { DeleteModal } from "@/components/DeleteModal";
import { useRouter } from "@/navigation";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { useState, useTransition } from "react";

export function DeleteTopicModal({
  topicName,
  onDelete,
}: {
  topicName: string;
  onDelete: () => Promise<void>;
}) {
  const t = useTranslations("delete-topic");
  const router = useRouter();
  const params = useParams<KafkaParams>();
  const [pending, startTransition] = useTransition();
  const [deleting, setDeleting] = useState(false);
  const isDeleting = deleting || pending;

  async function handleDelete() {
    try {
      setDeleting(true);
      await onDelete();
      startTransition(() => {
        router.push(`/kafka/${params.kafkaId}/topics/post-delete`);
      });
    } finally {
      setDeleting(false);
    }
  }

  function onCancel() {
    startTransition(() => {
      router.back();
    });
  }

  return (
    <DeleteModal
      variant={"destructive"}
      title={t("title")}
      confirmationValue={topicName}
      isModalOpen={true}
      isDeleting={isDeleting}
      appendTo={() => document.body}
      onDelete={handleDelete}
      onCancel={onCancel}
    >
      <>
        {t.rich("message", {
          topicName,
        })}
      </>
    </DeleteModal>
  );
}
