ARG log_level=info
ARG console_mode=read-only

FROM registry.access.redhat.com/ubi9/nodejs-18 AS deps
USER 0
WORKDIR /app
RUN npm install -g yarn
COPY package.json yarn.lock* package-lock.json* pnpm-lock.yaml* ./
RUN \
  if [ -f yarn.lock ]; then yarn --frozen-lockfile; \
  elif [ -f package-lock.json ]; then npm ci --omit=dev; \
  elif [ -f pnpm-lock.yaml ]; then yarn global add pnpm && pnpm i --frozen-lockfile; \
  else echo "Lockfile not found." && exit 1; \
  fi

FROM registry.access.redhat.com/ubi9/nodejs-18 AS builder
USER 0
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ARG backend_url=http://example
ARG console_metrics_prometheus_url=http://example
ARG nextauth_secret=examplesecret
ARG log_level
ARG console_mode

ENV BACKEND_URL=$backend_url
ENV CONSOLE_METRICS_PROMETHEUS_URL=$console_metrics_prometheus_url
ENV NEXTAUTH_SECRET=$nextauth_secret
ENV LOG_LEVEL=$log_level
ENV CONSOLE_MODE=$console_mode

RUN npm run build

FROM registry.access.redhat.com/ubi9/nodejs-18-minimal AS runner
USER 0
WORKDIR /app

COPY --from=builder /app/public ./public
COPY --from=builder --chown=1001:1001 /app/.next/standalone ./
COPY --from=builder --chown=1001:1001 /app/.next/static ./.next/static

ARG log_level
ARG console_mode

# Ensure set to some default for runtime
ENV LOG_LEVEL=$log_level
ENV CONSOLE_MODE=$console_mode

USER 1001

EXPOSE 3000

ENV NODE_ENV production
ENV PORT 3000

CMD ["node", "server.js"]
