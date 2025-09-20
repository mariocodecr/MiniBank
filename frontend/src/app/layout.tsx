import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { Providers } from '@/components/providers/providers';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'MiniBank - Digital Banking Platform',
  description: 'A production-grade digital banking platform with multi-currency support and real-time FX',
  keywords: ['banking', 'fintech', 'payments', 'foreign exchange', 'multi-currency'],
  authors: [{ name: 'MiniBank Team' }],
  creator: 'MiniBank',
  robots: {
    index: false, // Don't index in development
    follow: false,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={inter.className}>
        <Providers>
          <div className="min-h-screen bg-background">
            {children}
          </div>
        </Providers>
      </body>
    </html>
  );
}