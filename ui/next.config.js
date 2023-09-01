/** @type {import('next').NextConfig} */

const withNextIntl = require("next-intl/plugin")(
  // This is the default (also the `src` folder is supported out of the box)
  "./i18n.ts",
);

const nextConfig = {
  output: "standalone",
  experimental: {
    serverActions: true,
    // typedRoutes: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
};

module.exports = withNextIntl(nextConfig);
