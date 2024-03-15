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

ENV BACKEND_URL http://example
ENV CONSOLE_METRICS_PROMETHEUS_URL http://example
ENV NEXTAUTH_SECRET examplesecret
ENV LOG_LEVEL info
ENV CONSOLE_MODE read-only
ENV NEXT_TELEMETRY_DISABLED 1

RUN npm run build

FROM registry.access.redhat.com/ubi9/nodejs-18-minimal AS runner
USER 0
WORKDIR /app

ENV NODE_ENV production

COPY --from=builder /app/public ./public
COPY --from=builder --chown=1001:1001 /app/.next/standalone ./
COPY --from=builder --chown=1001:1001 /app/.next/static ./.next/static

USER 1001

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
