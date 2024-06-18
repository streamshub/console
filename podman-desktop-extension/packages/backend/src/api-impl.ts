/**********************************************************************
 * Copyright (C) 2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ***********************************************************************/

import { Messages } from '/@shared/src/messages/Messages';
import type { ConsoleCompose, ConsoleConfig, StreamshubConsoleInfo } from '/@shared/src/models/streamshub';
import type { StreamshubApi } from '/@shared/src/StreamshubApi';
import * as podmanDesktopApi from '@podman-desktop/api';
import { telemetryLogger } from './extension';
import { createAndStartConsole, stopConsole, stopAndDeleteConsole } from './utils';

const CONTAINER_LABEL = 'io.streamshub.console.container';

export class StreamshubImpl implements StreamshubApi {
  private webview: podmanDesktopApi.Webview;

  constructor(
    private readonly extensionContext: podmanDesktopApi.ExtensionContext,
    webview: podmanDesktopApi.Webview,
  ) {
    this.webview = webview;
  }


  async listConsoles(): Promise<StreamshubConsoleInfo[]> {
    const consoles: Record<string, StreamshubConsoleInfo> = {};
    try {
      const containers = await podmanDesktopApi.containerEngine.listContainers();

      const consoleContainers = containers.filter(
        container =>
          container.Labels['com.docker.compose.project'] !== undefined &&
          container.Labels[CONTAINER_LABEL] !== undefined,
      );
      consoleContainers
        .forEach(container => {
          const project = container.Labels['com.docker.compose.project'];
          const c: StreamshubConsoleInfo = consoles[project] ?? {
            project,
            api: {
              ports: [],
              baseUrl: '',
            },
            ui: {
              ports: [],
              url: '',
            },
          };
          if (container.Labels[CONTAINER_LABEL] === 'api') {
            c.api.ports = container.Ports;
            c.api.baseUrl = `http://localhost:${c.api.ports.find(p => p.PublicPort)?.PublicPort}`;
          }
          if (container.Labels[CONTAINER_LABEL] === 'ui') {
            c.ui.ports = container.Ports;
            c.ui.url = `http://localhost:${c.ui.ports.find(p => p.PublicPort)?.PublicPort}`;
          }
          console.log(c);
          consoles[project] = c;
        });
      console.log({ containers, consoleContainers, consoles });
    } catch (err) {
      await podmanDesktopApi.window.showErrorMessage(`Error listing containers: ${err}`);
      console.error('Error listing containers: ', err);
    }
    return Object.values(consoles);
  }


  async openLink(link: string): Promise<void> {
    console.log('openLink', link);
    await podmanDesktopApi.env.openExternal(podmanDesktopApi.Uri.parse(link));
  }


  // Log an event to telemetry
  async telemetryLogUsage(eventName: string, data?: Record<string, unknown>): Promise<void> {
    telemetryLogger.logUsage(eventName, data);
  }

  // Log an error to telemetry
  async telemetryLogError(eventName: string, data?: Record<string, unknown>): Promise<void> {
    telemetryLogger.logError(eventName, data);
  }

  async createConsole(projectName: string, compose: ConsoleCompose, config: ConsoleConfig): Promise<void> {
    const { storagePath } = this.extensionContext;
    console.log('createConsole', { projectName, compose, config, storagePath });
    await createAndStartConsole(storagePath, projectName, compose, config);
  }

  async stopConsole(projectName: string): Promise<void> {
    const { storagePath } = this.extensionContext;
    console.log('stopConsole', { storagePath, projectName });
    await stopConsole(storagePath, projectName);
  }

  async deleteConsole(projectName: string): Promise<void> {
    const { storagePath } = this.extensionContext;
    console.log('deleteConsole', { storagePath, projectName });
    await stopAndDeleteConsole(storagePath, projectName);

  }

  async containerChanges() {
    return this.notify(Messages.MSG_CONTAINERS_UPDATE);
  }

  // The API does not allow callbacks through the RPC, so instead
  // we send "notify" messages to the frontend to trigger a refresh
  // this method is internal and meant to be used by the API implementation
  protected async notify(msg: string, body?: unknown): Promise<void> {
    await this.webview.postMessage({
      id: msg,
      // Must pass in an empty body to satisfy the type system, if it is undefined, this fails.
      body: body ?? '',
    });
  }
}
