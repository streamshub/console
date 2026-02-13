# StreamsHub Console - React SPA POC

This is a proof-of-concept migration of the StreamsHub Console UI from Next.js to a traditional client-side React application.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Prerequisites

The Quarkus backend API must be running on `http://localhost:8080`:

```bash
cd ../api
mvn quarkus:dev
```

## Project Structure

```
poc-react/
├── src/
│   ├── main.tsx              # Entry point
│   ├── App.tsx               # Root component
│   ├── api/                  # API client layer
│   │   ├── client.ts         # Base API client
│   │   ├── hooks/            # TanStack Query hooks
│   │   └── types.ts          # API type definitions
│   ├── routes/               # Routing configuration
│   │   └── index.tsx         # Route definitions
│   ├── pages/                # Page components
│   ├── components/           # Reusable components
│   ├── i18n/                 # Internationalization
│   └── utils/                # Utility functions
├── public/                   # Static assets
├── index.html               # HTML entry point
├── vite.config.ts           # Vite configuration
├── tsconfig.json            # TypeScript configuration
└── package.json             # Dependencies
```

## Technology Stack

- **Build Tool:** Vite
- **Framework:** React 18 + TypeScript
- **Routing:** React Router v6
- **State Management:** TanStack Query + Zustand
- **i18n:** react-i18next
- **UI Components:** PatternFly React
- **Styling:** PatternFly CSS

## Development

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

### API Proxy

During development, API requests to `/api/*` are proxied to `http://localhost:8080` (configured in `vite.config.ts`).

## Migration Status

See [MIGRATION_PLAN.md](./MIGRATION_PLAN.md) for the full migration plan and progress.

### Current Status: Phase 1 - Foundation Setup

- [x] Project structure created
- [x] Configuration files set up
- [ ] Core dependencies installed
- [ ] API client implemented
- [ ] Routing configured
- [ ] First page migrated

## Next Steps

1. Install dependencies: `npm install`
2. Create source directory structure
3. Implement API client
4. Set up routing
5. Migrate first page (Home/Cluster selection)