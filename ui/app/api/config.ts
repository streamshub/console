"use server";

import * as yaml from 'js-yaml';

export interface OidcConfig {
    authServerUrl: string | null;
    clientId: string | null;
    clientSecret: string | null;
}

export interface GlobalSecurityConfig {
    oidc: OidcConfig | null;
}

export interface ConsoleConfig {
    security: GlobalSecurityConfig | null;
}

export default async function config(): Promise<ConsoleConfig> {
    return yaml.load(process.env.CONSOLE_CONFIG!) as ConsoleConfig;
}
