namespace NodeJS {
  interface ProcessEnv {
    NEXTAUTH_URL: string;
    NEXTAUTH_SECRET: string;
    BACKEND_URL: string;
    NEXT_PUBLIC_PRODUCTIZED_BUILD?: "true" | "false";
    LOG_LEVEL?: "fatal" | "error" | "warn" | "info" | "debug" | "trace";
    CONSOLE_MODE?: "read-only" | "read-write";
    CONSOLE_CONFIG_PATH: string;
    CONSOLE_SECURITY_OIDC_TRUSTSTORE?: string;
  }
}
