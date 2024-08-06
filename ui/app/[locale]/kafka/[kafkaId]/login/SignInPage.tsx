"use client";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  Alert,
  LoginForm,
  LoginFormProps,
  LoginMainFooterBandItem,
  LoginPage,
} from "@patternfly/react-core";
import { signIn } from "next-auth/react";
import { useTranslations } from "next-intl";
import { FormEvent, useState } from "react";

type SignInPageErrorParam =
  | "Signin"
  | "OAuthSignin"
  | "OAuthCallbackError"
  | "OAuthCreateAccount"
  | "EmailCreateAccount"
  | "Callback"
  | "OAuthAccountNotLinked"
  | "EmailSignin"
  | "CredentialsSignin"
  | "SessionRequired";

const signinErrors: Record<SignInPageErrorParam | "default" | string, string> =
  {
    default: "Unable to sign in.",
    Signin: "Try signing in with a different account.",
    OAuthSignin: "Try signing in with a different account.",
    OAuthCallbackError: "Try signing in with a different account.",
    OAuthCreateAccount: "Try signing in with a different account.",
    EmailCreateAccount: "Try signing in with a different account.",
    Callback: "Try signing in with a different account.",
    OAuthAccountNotLinked:
      "To confirm your identity, sign in with the same account you used originally.",
    EmailSignin: "The e-mail could not be sent.",
    CredentialsSignin:
      "Sign in failed. Check the details you provided are correct.",
    SessionRequired: "Please sign in to access this page.",
  };

export function SignInPage({ callbackUrl }: { callbackUrl: string }) {
  const t = useTranslations();
  const productName = t("common.product");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();

  const handleUsernameChange = (
    _event: FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setUsername(value);
  };

  const handlePasswordChange = (
    _event: FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setPassword(value);
  };

  const learnMoreResource = (
    <LoginMainFooterBandItem>
      <ExternalLink
        href={"https://redhat.com"}
        testId={"learn-more-about-streams-kafka"}
      >
        {t("login-in-page.learning_resource", { product: productName })}
      </ExternalLink>
    </LoginMainFooterBandItem>
  );

  const onSubmit: LoginFormProps["onSubmit"] = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsSubmitting(true);
    setError(undefined);
    const res = await signIn("credentials", {
      username,
      password,
      redirect: false,
    });
    if (res?.error) {
      const error = signinErrors[res.error] ?? signinErrors.default;
      setError(error);
    } else if (res?.ok) {
      window.location.href = callbackUrl ?? "/";
    }
    setIsSubmitting(false);
  };

  return (
    <LoginPage
      backgroundImgSrc="/assets/images/pfbg-icon.svg"
      loginTitle={t("homepage.page_header", { product: productName })}
      loginSubtitle={t("login-in-page.login_sub_title")}
      textContent={t("login-in-page.text_content", { product: productName })}
      brandImgSrc={"/StreamsLogo.png"}
      footerListItems={t("login-in-page.footer_text")}
      socialMediaLoginContent={learnMoreResource}
    >
      {error && isSubmitting === false && (
        <Alert variant={"danger"} isInline={true} title={error} />
      )}
      <LoginForm
        usernameLabel={t("login-in-page.username")}
        passwordLabel={t("login-in-page.password")}
        loginButtonLabel={t("login-in-page.login_button")}
        usernameValue={username}
        passwordValue={password}
        onChangeUsername={handleUsernameChange}
        onChangePassword={handlePasswordChange}
        onSubmit={onSubmit}
        isLoginButtonDisabled={isSubmitting}
      />
    </LoginPage>
  );
}
