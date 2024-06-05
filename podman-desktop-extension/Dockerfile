#
# Copyright (C) 2023 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

FROM registry.access.redhat.com/ubi9/nodejs-18:latest AS builder

USER root
RUN npm install -g yarn

COPY  . /extension
WORKDIR /extension

RUN  chgrp -R 0 /extension && chmod -R g+rwX /extension

USER 1001

RUN yarn install && yarn build

RUN mkdir /tmp/extension \
    && cp /extension/package.json \
          /extension/LICENSE      \
          /extension/README.md    \
          /extension/icon.png   /tmp/extension \
    && cp -r /extension/dist    /tmp/extension/dist

FROM scratch
LABEL org.opencontainers.image.title="Podman Desktop Demo" \
      org.opencontainers.image.description="Demo Podify scenario" \
      org.opencontainers.image.vendor="redhat" \
      io.podman-desktop.api.version=">= 1.5.0"
COPY --from=builder /tmp/extension/ /extension
