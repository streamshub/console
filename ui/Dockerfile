FROM registry.access.redhat.com/ubi9/nodejs-18-minimal
USER 0
WORKDIR /app

COPY public                             ./public
COPY --chown=1001:1001 .next/standalone ./
COPY --chown=1001:1001 .next/static     ./.next/static

ARG log_level=info
ARG console_mode=read-only

# Ensure set to some default for runtime
ENV LOG_LEVEL=$log_level
ENV CONSOLE_MODE=$console_mode

USER 1001

EXPOSE 3000

ENV NODE_ENV=production
ENV PORT=3000

CMD ["node", "server.js"]
