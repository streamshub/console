import { getCsrfToken, getProviders } from "next-auth/react";
import { SignInPage } from "./SignInPage";

export default async function SignIn({
  searchParams,
}: {
  searchParams?: { callbackUrl?: string };
}) {
  return <SignInPage callbackUrl={searchParams?.callbackUrl ?? "/"} />;
}
