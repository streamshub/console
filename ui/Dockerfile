FROM registry.access.redhat.com/ubi9/nodejs-22-minimal

WORKDIR /app
EXPOSE 3000

ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
ENV LOG_LEVEL=info

CMD ["node", "server.js"]

USER 0

COPY public                             ./public
COPY --chown=1001:1001 .next/standalone ./
COPY --chown=1001:1001 .next/static     ./.next/static

USER 1001
