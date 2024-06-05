/**********************************************************************
 * Copyright (C) 2023 Red Hat, Inc.
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

import type { WebviewPanel } from '@podman-desktop/api';
import * as extensionApi from '@podman-desktop/api';

export class StreamsConsoleExtension {
  private debounceTimeout: NodeJS.Timeout | undefined;
  private readonly panel: WebviewPanel | undefined;
  private uiPort: number | undefined;

  constructor(private readonly extensionContext: extensionApi.ExtensionContext) {
    if (!this.panel) {
      this.panel = extensionApi.window.createWebviewPanel('streams-console', 'streams for Apache Kafka', {});
    }
  }

  getPodmanConnection(): extensionApi.ContainerProviderConnection {
    // get all engines
    const providerConnections = extensionApi.provider.getContainerConnections();

    // keep only the podman engine
    const podmanConnection = providerConnections.filter(
      providerConnection => providerConnection.connection.type === 'podman',
    );

    // engine running
    if (podmanConnection.length < 1) {
      throw new Error('No podman engine running.');
    }

    // get the podman engine
    return podmanConnection[0].connection;
  }

  async getFrontendContainerPodified(): Promise<extensionApi.ContainerInfo | undefined> {
    const containers = await extensionApi.containerEngine.listContainers();
    containers.forEach(c => console.log(c));

    const frontendContainers = containers.filter(
      container =>
        container.Image === 'localhost/console-ui:local' &&
        container.engineType === 'podman' &&
        container.Names[0] === '/console-ui',
    );
    if (frontendContainers.length < 1) {
      return undefined;
    }
    return frontendContainers[0];
  }

  checkCurrentStateInBackground(): void {
    this.checkCurrentState().catch((error: unknown) => {
      console.error('Error while checking current state', error);
    });
  }

  async checkCurrentState(): Promise<void> {
    const container = await this.getFrontendContainerPodified();
    const uiPort = container?.Ports[0]?.PublicPort;
    if (container !== undefined) {
      if (this.uiPort !== uiPort) {
        this.loadConsoleUI(uiPort);
      }
      this.uiPort = uiPort;
    } else {
      this.loadWelcomeScreen();
      this.uiPort = undefined;
    }
  }

  loadWelcomeScreen(): void {
    this.panel.webview.html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Security-Policy" content="default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;">
</head>
<body>
    No console detected!
</body>
</html>
    `;
  }

  loadConsoleUI(publicPort: number): void {
    this.panel.webview.html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Security-Policy" content="default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;">
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden; /* Prevent scrollbars */
        }
        iframe {
            width: 100%;
            height: 100%;
            border: none; /* Remove iframe border */
        }
    </style>
</head>
<body>
    <iframe src="http://localhost:${publicPort}"></iframe>
</body>
</html>
    `;
  }

  init() {
    this.checkCurrentStateInBackground();

    // trigger the check on any change
    this.extensionContext.subscriptions.push(
      extensionApi.containerEngine.onEvent(() => {
        // cancel previous timeout
        if (this.debounceTimeout) {
          clearTimeout(this.debounceTimeout);
        }

        // handle it after a while
        this.debounceTimeout = setTimeout(() => {
          this.checkCurrentStateInBackground();
        }, 1000);
      }),
    );
  }

  tearDown() {
    this.panel.dispose();
  }
}
