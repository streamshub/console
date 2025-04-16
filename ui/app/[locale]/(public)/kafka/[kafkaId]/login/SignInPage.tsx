"use client";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import { Link } from "@/i18n/routing";
import {
  Alert,
  Button,
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

export function SignInPage({
  kafkaId,
  provider,
  callbackUrl,
  hasMultipleClusters,
}: {
  kafkaId: string,
  provider: "credentials" | "oauth-token" | "anonymous";
  callbackUrl: string;
  hasMultipleClusters: boolean;
}) {
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

  const doLogin = async () => {
    setIsSubmitting(true);
    setError(undefined);
    const options = {
      credentials: { username, password },
      "oauth-token": { clientId: username, secret: password },
      anonymous: {},
    }[provider];
    const providerId = provider === "credentials" ? "credentials-" + kafkaId : provider;
    const res = await signIn(providerId, {
      ...options,
      redirect: false,
    });
    if (res?.error) {
      const error = signinErrors[res.error] ?? signinErrors.default;
      setError(error);
      setIsSubmitting(false);
    } else if (res?.ok) {
      window.location.href = callbackUrl;
    } else {
      setIsSubmitting(false);
    }
  };

  const onSubmit: LoginFormProps["onSubmit"] = (e) => {
    e.preventDefault();
    e.stopPropagation();
    void doLogin();
  };

  const usernameLabel =
    provider === "credentials"
      ? t("login-in-page.username")
      : t("login-in-page.clientId");
  const passwordLabel =
    provider === "credentials"
      ? t("login-in-page.password")
      : t("login-in-page.clientSecret");

  return (
    <LoginPage
      backgroundImgSrc="/assets/images/pfbg-icon.svg"
      loginTitle={t("homepage.page_header", { product: productName })}
      loginSubtitle={t("login-in-page.login_sub_title")}
      textContent={t("login-in-page.text_content", { product: productName })}
      brandImgSrc={"/full-logo.svg"}
      footerListItems={t("login-in-page.footer_text")}
      socialMediaLoginContent={learnMoreResource}
      signUpForAccountMessage={
        hasMultipleClusters && (
          <Link href={"/"}>
            {t("login-in-page.log_into_a_different_cluster")}
          </Link>
        )
      }
    >
      {error && isSubmitting === false && (
        <Alert variant={"danger"} isInline={true} title={error} />
      )}

      {provider === "anonymous" ? (
        <Button onClick={doLogin}>Click to login anonymously</Button>
      ) : (
        <LoginForm
          usernameLabel={usernameLabel}
          passwordLabel={passwordLabel}
          loginButtonLabel={t("login-in-page.login_button")}
          usernameValue={username}
          passwordValue={password}
          onChangeUsername={handleUsernameChange}
          onChangePassword={handlePasswordChange}
          onSubmit={onSubmit}
          isLoginButtonDisabled={isSubmitting}
        />
      )}
    </LoginPage>
  );
}
