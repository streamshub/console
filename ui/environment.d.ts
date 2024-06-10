namespace NodeJS {
  interface ProcessEnv {
    NEXTAUTH_URL: string;
    NEXTAUTH_SECRET: string;
    BACKEND_URL: string;
    NEXT_PUBLIC_PRODUCTIZED_BUILD?: "true" | "false";
    LOG_LEVEL?: "fatal" | "error" | "warn" | "info" | "debug" | "trace";
    CONSOLE_METRICS_PROMETHEUS_URL?: string;
    CONSOLE_MODE?: "read-only" | "read-write";
    CONSOLE_METRICS_PROMETHEUS_URL?: string;
    CONSOLE_OAUTH_PROVIDER?: string;
    CONSOLE_OAUTH_PROVIDER_NAME?: string;
    CONSOLE_OAUTH_CLIENT_ID?: string;
    CONSOLE_OAUTH_CLIENT_SECRET?: string;
    CONSOLE_OAUTH_AUTHORIZATION_URL?: string;
    CONSOLE_OAUTH_TOKEN_URL?: string;
    CONSOLE_OAUTH_USERINFO_URL?: string;
    CONSOLE_OAUTH_AUTHORIZATION_SCOPE?: string;
    CONSOLE_OAUTH_ISSUER?: string;
  }
}
