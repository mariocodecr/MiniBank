'use client';

import Link from 'next/link';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  Bell, 
  Users, 
  Shield, 
  Database, 
  Activity, 
  Settings, 
  ArrowRight 
} from 'lucide-react';

const adminModules = [
  {
    title: 'Notifications',
    description: 'Manage system notification preferences and delivery channels',
    href: '/admin/notifications',
    icon: Bell,
    status: 'Active',
    statusColor: 'success' as const,
  },
  {
    title: 'User Management',
    description: 'Manage user accounts, roles, and permissions',
    href: '/admin/users',
    icon: Users,
    status: 'Coming Soon',
    statusColor: 'secondary' as const,
  },
  {
    title: 'Security Settings',
    description: 'Configure authentication and security policies',
    href: '/admin/security',
    icon: Shield,
    status: 'Coming Soon',
    statusColor: 'secondary' as const,
  },
  {
    title: 'Database Management',
    description: 'Monitor database health and run maintenance tasks',
    href: '/admin/database',
    icon: Database,
    status: 'Coming Soon',
    statusColor: 'secondary' as const,
  },
  {
    title: 'System Monitoring',
    description: 'View system metrics, logs, and performance data',
    href: '/admin/monitoring',
    icon: Activity,
    status: 'Coming Soon',
    statusColor: 'secondary' as const,
  },
  {
    title: 'System Configuration',
    description: 'Manage application settings and feature flags',
    href: '/admin/config',
    icon: Settings,
    status: 'Coming Soon',
    statusColor: 'secondary' as const,
  },
];

export default function AdminPage() {
  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Administration</h1>
          <p className="text-muted-foreground">
            Manage system settings, users, and monitoring
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {adminModules.map((module) => (
            <Card key={module.href} className="relative">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="flex items-center space-x-2">
                  <module.icon className="h-5 w-5 text-muted-foreground" />
                  <CardTitle className="text-lg">{module.title}</CardTitle>
                </div>
                <Badge variant={module.statusColor}>
                  {module.status}
                </Badge>
              </CardHeader>
              <CardContent>
                <CardDescription className="mb-4">
                  {module.description}
                </CardDescription>
                {module.status === 'Active' ? (
                  <Button asChild className="w-full">
                    <Link href={module.href as any}>
                      Open Module
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                ) : (
                  <Button variant="outline" className="w-full" disabled>
                    Coming Soon
                  </Button>
                )}
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Quick Stats */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">System Status</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">Healthy</div>
              <p className="text-xs text-muted-foreground">
                All services operational
              </p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Active Users</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">1,234</div>
              <p className="text-xs text-muted-foreground">
                +12% from last month
              </p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Notifications Sent</CardTitle>
              <Bell className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">8,521</div>
              <p className="text-xs text-muted-foreground">
                Today
              </p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Database Size</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">2.4 GB</div>
              <p className="text-xs text-muted-foreground">
                +0.2 GB this week
              </p>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>
              Latest administrative actions and system events
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex items-center space-x-3 text-sm">
                <Badge variant="outline">System</Badge>
                <span>Database backup completed successfully</span>
                <span className="text-muted-foreground ml-auto">2 hours ago</span>
              </div>
              <div className="flex items-center space-x-3 text-sm">
                <Badge variant="outline">User</Badge>
                <span>New user registration: john.doe@example.com</span>
                <span className="text-muted-foreground ml-auto">4 hours ago</span>
              </div>
              <div className="flex items-center space-x-3 text-sm">
                <Badge variant="outline">Security</Badge>
                <span>Failed login attempt detected and blocked</span>
                <span className="text-muted-foreground ml-auto">6 hours ago</span>
              </div>
              <div className="flex items-center space-x-3 text-sm">
                <Badge variant="outline">System</Badge>
                <span>Notification service restarted</span>
                <span className="text-muted-foreground ml-auto">1 day ago</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  );
}