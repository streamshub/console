namespace NodeJS {
  interface ProcessEnv {
    NEXTAUTH_URL: string;
    NEXTAUTH_SECRET: string;
    SESSION_SECRET: string;
    BACKEND_URL: string;
    KEYCLOAK_CLIENTID: string;
    KEYCLOAK_CLIENTSECRET: string;
    KEYCLOAK_ISSUER: string;
  }
}
