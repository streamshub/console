This part of the project contains the user interface for the Kafka console, developed using Next.js and the [PatternFly](https://patternfly.org) UI library.
## Getting Started

Create a `.env` file containing the details about where to find the API server, and some additional config.

```.dotenv
BACKEND_URL=http://localhost:8080
CONSOLE_METRICS_PROMETHEUS_URL=http://localhost:9090
NEXTAUTH_SECRET=abcdefghijklmnopqrstuvwxyz1234567890=
LOG_LEVEL=info
```

[!WARNING]
Please generate a valid and secure value for `NEXTAUTH_SECRET`. We suggest running `openssl rand -base64 32` to get started.

Then run the application.

```bash
npm run install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

