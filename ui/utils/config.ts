"use server";

import fs from "fs";
import * as yaml from "js-yaml";
import { logger } from "@/utils/logger";

const log = logger.child({ module: "utils" });

export interface TrustStore {
  type: string;
  content: {
    value?: string | null;
    valueFrom?: string | null;
  };
}

export interface OidcConfig {
  authServerUrl: string | null;
  clientId: string | null;
  clientSecret: string | null;
  trustStore: TrustStore | null;
  scopes: string | null;
}

export interface GlobalSecurityConfig {
  oidc: OidcConfig | null;
}

export interface ConsoleConfig {
  readOnly: boolean;
  techPreview: boolean;
  showLearning: boolean;
  security: GlobalSecurityConfig | null;
  loadTime: Date;
}

/**
 * Determine if the configuration should be reloaded. This is needed when it has not yet been loaded,
 * the configuration file has changed on disk, or one of the environment variables has been modified
 * (dev mode only).
 */
function reloadConfig(
  consoleConfig: ConsoleConfig,
  readOnly: boolean,
  techPreview: boolean,
  showLearning: boolean,
) {
  if (consoleConfig === undefined) {
    return true;
  }

  let stats =
    process.env.CONSOLE_CONFIG_PATH &&
    fs.statSync(process.env.CONSOLE_CONFIG_PATH);

  if (stats && stats.mtime > consoleConfig.loadTime) {
    log.info(
      `reloading configuration from ${process.env.CONSOLE_CONFIG_PATH} due to file modification`,
    );
    return true;
  }

  if (consoleConfig.readOnly != readOnly) {
    log.info(`reloading configuration due to updated env`);
    return true;
  }

  if (consoleConfig.techPreview != techPreview) {
    log.info(`reloading configuration due to updated env`);
    return true;
  }

  if (consoleConfig.showLearning != showLearning) {
    log.info(`reloading configuration due to updated env`);
    return true;
  }

  return false;
}

/**
 * Check whether the configuration value contains a reference to an environment
 * variable - e.g. ${my-var-reference}. To find the environment variable,
 * non-word [^a-zA-Z_0-9] characters are replaced by underscore and the entire
 * value is converted to upper-case characters.
 */
function resolveValue(value: string | null): string | null {
    if (value?.startsWith("${") && value.endsWith("}")) {
        let envName = value.substring(2, value.length - 1).replaceAll(/\W/g, "_").toUpperCase();
        log.debug(`looking for ${envName} in process.env`);
        return process.env[envName] ?? value;
    }

    return value;
}

async function getOrLoadConfig(): Promise<ConsoleConfig> {
  let consoleConfig: ConsoleConfig = (globalThis as any).consoleConfig;
  let readOnly: boolean =
    (process.env.CONSOLE_MODE ?? "read-write") == "read-only";
  let techPreview: boolean =
    (process.env.CONSOLE_TECH_PREVIEW ?? "false") == "true";
  let showLearning: boolean =
    (process.env.CONSOLE_SHOW_LEARNING ?? "true") == "true";

  if (reloadConfig(consoleConfig, readOnly, techPreview, showLearning)) {
    if (!process.env.CONSOLE_CONFIG_PATH) {
      log.warn(
        "console configuration path variable CONSOLE_CONFIG_PATH is not set, configuration not loaded",
      );
      consoleConfig = {
        readOnly: readOnly,
        techPreview: techPreview,
        showLearning: showLearning,
        security: { oidc: null },
        loadTime: new Date(),
      };
    } else {
      const fileContents = fs.readFileSync(
        process.env.CONSOLE_CONFIG_PATH,
        "utf8",
      );
      const cfg = yaml.load(fileContents) as ConsoleConfig;
      const trustStoreCfg = cfg.security?.oidc?.trustStore ?? null;
      let trustStore: TrustStore | null = null;

      if (trustStoreCfg?.type == "PEM") {
        if (trustStoreCfg.content.value) {
          trustStore = {
            type: resolveValue(trustStoreCfg.type)!,
            content: {
              value: resolveValue(trustStoreCfg.content.value),
            },
          };
        } else if (trustStoreCfg.content.valueFrom) {
          trustStore = {
            type: resolveValue(trustStoreCfg.type)!,
            content: {
              value: fs.readFileSync(
                resolveValue(trustStoreCfg.content.valueFrom)!,
                "utf8"
              ),
            },
          };
        }
      } else if (trustStoreCfg?.type !== undefined) {
        log.warn(
          "console configuration with OIDC non-PEM truststore is not supported",
        );
      }

      consoleConfig = {
        readOnly: readOnly,
        techPreview: techPreview,
        showLearning: showLearning,
        security: {
          oidc:
            cfg.security?.oidc == null
              ? null
              : {
                  authServerUrl: resolveValue(cfg.security?.oidc?.authServerUrl ?? null),
                  clientId: resolveValue(cfg.security?.oidc?.clientId ?? null),
                  clientSecret: resolveValue(cfg.security?.oidc?.clientSecret ?? null),
                  trustStore: trustStore,
                  scopes: resolveValue(cfg.security?.oidc?.scopes),
                },
        },
        loadTime: new Date(),
      };
      if (log.isLevelEnabled("debug")) {
        log.debug(
          consoleConfig,
          `console configuration loaded from ${process.env.CONSOLE_CONFIG_PATH}`,
        );
      } else {
        log.info(
          `console configuration loaded from ${process.env.CONSOLE_CONFIG_PATH}`,
        );
      }
    }

    (globalThis as any).consoleConfig = consoleConfig;
  } else {
    log.trace(
      consoleConfig,
      `console configuration reused from globalThis`,
    );
  }

  return consoleConfig;
}

export default async function config(): Promise<ConsoleConfig> {
  return getOrLoadConfig();
}

/**
 * Fetch configuration safe to use from client-side code that does not include security
 * information.
 */
export async function clientConfig(): Promise<ConsoleConfig> {
  return getOrLoadConfig().then((cfg) => {
    return {
      ...cfg,
      // Do not return security information do client-side
      security: null,
    };
  });
}

export async function oidcEnabled(): Promise<boolean> {
  return getOrLoadConfig().then((cfg) => {
    return (cfg?.security?.oidc ?? null) !== null;
  });
}
