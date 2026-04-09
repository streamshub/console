import { ReactNode, Suspense } from "react";

type Props = {
  children: ReactNode;
};

//export const fetchCache = "force-no-store";
//export const dynamic = "force-dynamic";

// Since we have a `not-found.tsx` page on the root, a layout file
// is required, even if it's just passing children through.
export default function RootLayout({ children }: Props) {
  return (
    <html lang="en">
      <body>
        <Suspense fallback={<div>Loading...</div>}>
          <div id="root">{children}</div>
        </Suspense>
      </body>
    </html>
  );
}
