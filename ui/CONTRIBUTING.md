# Contributing to the UI

This part of the project contains the user interface for the Kafka console, developed using Next.js and the [PatternFly](https://patternfly.org) UI library.

## Prerequisites

Please make sure you have working installations of:

- node (v18+)
- npm (v10+)

To run a development version of the UI working in all its sections, you will need to install the console on a development cluster first. Please refer to the [install/README.md](../install/README.md) file for detailed instructions about how to do it.

Alternatively, you can run the API module locally, but sections depending on the metrics exported on Prometheus will not work correctly.

## Getting Started

Create a `.env` file containing the details about where to find the API server, and some additional config.

```.dotenv
# the actual URLs will depend on how you installed the console
BACKEND_URL=http://api.my-cluster
LOG_LEVEL=info
```

Install the required dependencies.

```bash
npm run install
```

Then run the application.

```bash
npm run build
npm run start
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the console user interface.

## Build

```bash
npm run build
```

This will create an optimized version of the application that can be deployed.

## Developing the UI

```bash
npm run dev
```

This will start the UI in dev mode, hosted (by default) on port 3000. When running successfully, you should see output similar to:

```
strimzi-ui@0.1.0 dev
NODE_OPTIONS='-r next-logger' next dev | pino-pretty

[16:11:06.206] INFO (console/14593):    ▲ Next.js 14.1.4
[16:11:06.206] INFO (console/14593):    - Local:        http://localhost:3000
[16:11:06.206] INFO (console/14593):    - Environments: .env.local
[16:11:06.206] INFO (next.js/14593):
prefix: "info"
[16:11:08.981] INFO (next.js/14593): Ready in 2.9s
prefix: "event"
```

You can then access the UI on port 3000 or your localhost.

Note: you will need the REST API running for the UI to work. See the README at the root of this repository for examples of how to do that.

### Develop the components in isolation using Storybook

```bash
npm run storybook
```

This will start Storybook, hosted on port 6006.

### Test the UI

```bash
npm run build-storybook
http-serve -p 6006 storybook-static

# in a different terminal
npm run test-storybook
```

This will build Storybook and run all the relative unit tests.

```bash
npm run build
npm run test
```

This will run Playwright against the built application.
