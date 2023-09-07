import { redirect } from "next/navigation";

export default function TopicPage({
  params,
}: {
  params: { bookmark: string; topic: string };
}) {
  redirect(`${params.topic}/overview`);
}
