FROM registry.access.redhat.com/ubi9/nodejs-18 AS deps
USER 0
WORKDIR /app
COPY package.json package-lock.json* ./
RUN \
  if [ -f package-lock.json ]; then npm ci --omit=dev; \
  else echo "Lockfile not found." && exit 1; \
  fi

RUN npm exec playwright install --with-deps

FROM registry.access.redhat.com/ubi9/nodejs-18 AS builder
USER 0
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED 1
RUN npm run build
RUN npm run test

FROM registry.access.redhat.com/ubi9/nodejs-18-minimal AS runner
USER 0
WORKDIR /app

ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

COPY --from=builder /app/public ./public
COPY --from=builder --chown=1001:1001 /app/.next/standalone ./
COPY --from=builder --chown=1001:1001 /app/.next/static ./.next/static

USER 1001

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
