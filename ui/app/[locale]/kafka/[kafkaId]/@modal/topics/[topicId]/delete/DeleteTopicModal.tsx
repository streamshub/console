"use client";
import { DeleteModal } from "@/components/DeleteModal";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

export function DeleteTopicModal({
  topicName,
  onDelete,
}: {
  topicName: string;
  onDelete: () => Promise<boolean>;
}) {
  const t = useTranslations("delete-topic");
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [deleting, setDeleting] = useState(false);
  const isDeleting = deleting || pending;

  async function handleDelete() {
    try {
      setDeleting(true);
      const res = await onDelete();
      if (res) {
        startTransition(() => {
          router.back();
        });
      }
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
