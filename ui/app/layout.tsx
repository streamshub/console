import { ReactNode } from "react";

type Props = {
  children: ReactNode;
};

export const fetchCache = "only-no-store";

// Since we have a `not-found.tsx` page on the root, a layout file
// is required, even if it's just passing children through.
export default function RootLayout({ children }: Props) {
  return (
    <html lang="en">
      <body>
        <div id="root">{children}</div>
      </body>
    </html>
  );
}
