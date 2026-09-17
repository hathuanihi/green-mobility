import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Green Mobility - Admin Portal",
  description: "Nền tảng quản trị điều hành và báo cáo phát thải xe điện Green Mobility",
};

import { AuthProvider } from "@/lib/auth";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body className="antialiased min-h-screen bg-slate-950 text-slate-50">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
