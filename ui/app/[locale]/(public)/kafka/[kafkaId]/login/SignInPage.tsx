'use client'
import { ThemeSelector } from '@/app/[locale]/ThemeSelector'
import { useColorTheme } from '@/app/[locale]/useColorTheme'
import { ExternalLink } from '@/components/Navigation/ExternalLink'
import RichText from '@/components/RichText'
import { Link } from '@/i18n/routing'
import {
  Alert,
  Button,
  Content,
  Flex,
  FlexItem,
  ListVariant,
  LoginFooter,
  LoginFooterItem,
  LoginForm,
  LoginFormProps,
  LoginMainFooterBandItem,
  LoginPage,
} from '@/libs/patternfly/react-core'
import { AngleLeftIcon } from '@/libs/patternfly/react-icons'
import { signIn } from 'next-auth/react'
import { useTranslations } from 'next-intl'
import { FormEvent, useEffect, useState } from 'react'

type SignInPageErrorParam =
  | 'Signin'
  | 'OAuthSignin'
  | 'OAuthCallbackError'
  | 'OAuthCreateAccount'
  | 'EmailCreateAccount'
  | 'Callback'
  | 'OAuthAccountNotLinked'
  | 'EmailSignin'
  | 'CredentialsSignin'
  | 'SessionRequired'

const signinErrors: Record<
  SignInPageErrorParam | 'default' | string,
  string
> = {
  default: 'Unable to sign in.',
  Signin: 'Try signing in with a different account.',
  OAuthSignin: 'Try signing in with a different account.',
  OAuthCallbackError: 'Try signing in with a different account.',
  OAuthCreateAccount: 'Try signing in with a different account.',
  EmailCreateAccount: 'Try signing in with a different account.',
  Callback: 'Try signing in with a different account.',
  OAuthAccountNotLinked:
    'To confirm your identity, sign in with the same account you used originally.',
  EmailSignin: 'The e-mail could not be sent.',
  CredentialsSignin:
    'Sign in failed. Check the details you provided are correct.',
  SessionRequired: 'Please sign in to access this page.',
}

export function SignInPage({
  kafkaId,
  provider,
  callbackUrl,
  hasMultipleClusters,
  clusterName,
}: {
  kafkaId: string
  provider: 'credentials' | 'oauth-token' | 'anonymous'
  callbackUrl: string
  hasMultipleClusters: boolean
  clusterName: string
}) {
  const t = useTranslations()
  const productName = t('common.product')

  const { isDarkMode } = useColorTheme()
  const [mounted, setMounted] = useState(false)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | undefined>()

  const [previousClusterId, setPreviousClusterId] = useState<string | null>(
    null,
  )
  const [previousClusterName, setPreviousClusterName] = useState<string | null>(
    null,
  )

  useEffect(() => {
    setMounted(true)
    const prevId = localStorage.getItem('PreviousClusterId')
    const prevName = localStorage.getItem('PreviousClusterName')
    if (prevId && prevName) {
      setPreviousClusterId(prevId)
      setPreviousClusterName(prevName)
    }
  }, [])

  const handleUsernameChange = (
    _event: FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setUsername(value)
  }

  const handlePasswordChange = (
    _event: FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    setPassword(value)
  }

  const learnMoreResource = (
    <LoginFooter>
      <Flex
        direction={{ default: 'column' }}
        justifyContent={{ default: 'justifyContentSpaceBetween' }}
      >
        <FlexItem>
          <Content>{t('login-in-page.footer_text')}</Content>
        </FlexItem>
        <FlexItem>
          <LoginFooterItem>
            <ExternalLink
              href={'https://redhat.com'}
              testId={'learn-more-about-streams-kafka'}
            >
              {t('login-in-page.learning_resource', { product: productName })}
            </ExternalLink>
          </LoginFooterItem>
        </FlexItem>
      </Flex>
    </LoginFooter>
  )

  const doLogin = async () => {
    setIsSubmitting(true)
    setError(undefined)
    const options = {
      credentials: { username, password },
      'oauth-token': { clientId: username, secret: password },
      anonymous: {},
    }[provider]
    const providerId =
      provider === 'credentials' ? 'credentials-' + kafkaId : provider
    const res = await signIn(providerId, {
      ...options,
      redirect: false,
    })
    if (res?.error) {
      const error = signinErrors[res.error] ?? signinErrors.default
      setError(error)
      setIsSubmitting(false)
    } else if (res?.ok) {
      window.location.href = callbackUrl
    } else {
      setIsSubmitting(false)
    }
  }

  const onSubmit: LoginFormProps['onSubmit'] = (e) => {
    e.preventDefault()
    e.stopPropagation()
    void doLogin()
  }

  const usernameLabel =
    provider === 'credentials'
      ? t('login-in-page.username')
      : t('login-in-page.clientId')
  const passwordLabel =
    provider === 'credentials'
      ? t('login-in-page.password')
      : t('login-in-page.clientSecret')

  const loginSubtitle =
    previousClusterId && previousClusterName
      ? provider === 'credentials'
        ? t('login-in-page.login_subtitle_credentials', { clusterName })
        : provider === 'oauth-token'
        ? t('login-in-page.login_subtitle_oauth', { clusterName })
        : t('login-in-page.login_sub_title')
      : t('login-in-page.login_sub_title')

  const brandImg = !mounted
    ? '/full_logo_hori_default.svg'
    : isDarkMode
    ? '/full_logo_hori_reverse.svg'
    : '/full_logo_hori_default.svg'

  return (
    <>
      <Flex
        justifyContent={{ default: 'justifyContentFlexEnd' }}
        className="pf-v6-u-mr-4xl pf-v6-u-mt-lg"
      >
        <FlexItem>{mounted && <ThemeSelector />}</FlexItem>
      </Flex>
      <LoginPage
        backgroundImgSrc="/assets/images/pfbg-icon.svg"
        loginTitle={t('homepage.page_header', { product: productName })}
        loginSubtitle={loginSubtitle}
        textContent={t('login-in-page.text_content', { product: productName })}
        brandImgSrc={brandImg}
        footerListItems={learnMoreResource}
        footerListVariants={ListVariant.inline}
        signUpForAccountMessage={
          hasMultipleClusters && (
            <LoginMainFooterBandItem>
              <Link href={'/'}>
                {t('login-in-page.log_into_a_different_cluster')}
              </Link>
            </LoginMainFooterBandItem>
          )
        }
        forgotCredentials={
          hasMultipleClusters &&
          previousClusterId &&
          previousClusterName &&
          previousClusterId !== kafkaId && (
            <LoginMainFooterBandItem>
              <Link href={`/kafka/${previousClusterId}/overview`}>
                <AngleLeftIcon />{' '}
                <RichText>
                  {(tags) =>
                    t.rich('login-in-page.stay_logged_in', {
                      ...tags,
                      clustername: previousClusterName,
                    })
                  }
                </RichText>
              </Link>
            </LoginMainFooterBandItem>
          )
        }
      >
        {error && isSubmitting === false && (
          <Alert variant={'danger'} isInline={true} title={error} />
        )}

        {provider === 'anonymous' ? (
          <Button ouiaId={'login-button'} onClick={doLogin}>
            {t('login-in-page.login_anonymously')}
          </Button>
        ) : (
          <LoginForm
            usernameLabel={usernameLabel}
            passwordLabel={passwordLabel}
            loginButtonLabel={t('login-in-page.login_button')}
            usernameValue={username}
            passwordValue={password}
            onChangeUsername={handleUsernameChange}
            onChangePassword={handlePasswordChange}
            onSubmit={onSubmit}
            isLoginButtonDisabled={isSubmitting}
          />
        )}
      </LoginPage>
    </>
  )
}
