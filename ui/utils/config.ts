"use server";

import fs from 'fs';
import * as yaml from 'js-yaml';
import { logger } from "@/utils/logger";

const log = logger.child({ module: "utils" });

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

async function getOrLoadConfig(): Promise<ConsoleConfig> {
    let consoleConfig: ConsoleConfig = (globalThis as any).consoleConfig;

    if (consoleConfig === undefined) {
        if (!process.env.CONSOLE_CONFIG_PATH) {
            log.warn("console configuration path variable CONSOLE_CONFIG_PATH is not set, configuration not loaded");
            consoleConfig = { security: { oidc: null } };
        } else {
            const fileContents = fs.readFileSync(process.env.CONSOLE_CONFIG_PATH, 'utf8');
            const cfg = yaml.load(fileContents) as ConsoleConfig;
            log.trace("console configuration loaded");

            consoleConfig = {
                security: {
                    oidc: {
                        authServerUrl: cfg.security?.oidc?.authServerUrl ?? null,
                        clientId: cfg.security?.oidc?.clientId ?? null,
                        clientSecret: cfg.security?.oidc?.clientSecret ?? null,
                    }
                }
            };
        }

        (globalThis as any).consoleConfig = consoleConfig;
    } else {
        log.trace("console configuration reused from globalThis");
    }

    return consoleConfig;
}

export default async function config(): Promise<ConsoleConfig> {
    return getOrLoadConfig();
}
