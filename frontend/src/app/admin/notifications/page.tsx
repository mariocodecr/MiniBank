'use client';

import { useState } from 'react';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Bell, Mail, MessageSquare, Phone, Save } from 'lucide-react';
import { toast } from 'sonner';

interface NotificationPreference {
  id: string;
  type: 'email' | 'sms' | 'push' | 'in-app';
  label: string;
  description: string;
  enabled: boolean;
  icon: React.ReactNode;
}

export default function AdminNotificationsPage() {
  const [preferences, setPreferences] = useState<NotificationPreference[]>([
    {
      id: 'payment_completed',
      type: 'email',
      label: 'Payment Completed',
      description: 'Notify when payments are successfully completed',
      enabled: true,
      icon: <Mail className="h-4 w-4" />,
    },
    {
      id: 'payment_failed',
      type: 'email',
      label: 'Payment Failed',
      description: 'Notify when payments fail or require attention',
      enabled: true,
      icon: <Mail className="h-4 w-4" />,
    },
    {
      id: 'account_low_balance',
      type: 'sms',
      label: 'Low Balance Alert',
      description: 'SMS alert when account balance falls below threshold',
      enabled: false,
      icon: <Phone className="h-4 w-4" />,
    },
    {
      id: 'fx_rate_change',
      type: 'push',
      label: 'FX Rate Changes',
      description: 'Push notifications for significant exchange rate changes',
      enabled: true,
      icon: <Bell className="h-4 w-4" />,
    },
    {
      id: 'system_maintenance',
      type: 'in-app',
      label: 'System Maintenance',
      description: 'In-app notifications for scheduled maintenance',
      enabled: true,
      icon: <MessageSquare className="h-4 w-4" />,
    },
    {
      id: 'security_alerts',
      type: 'email',
      label: 'Security Alerts',
      description: 'Email notifications for security-related events',
      enabled: true,
      icon: <Mail className="h-4 w-4" />,
    },
  ]);

  const handleToggle = (id: string) => {
    setPreferences(prev => 
      prev.map(pref => 
        pref.id === id 
          ? { ...pref, enabled: !pref.enabled }
          : pref
      )
    );
  };

  const handleSave = () => {
    // In a real implementation, this would call the backend
    toast.success('Notification preferences saved successfully');
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'email': return 'default';
      case 'sms': return 'secondary';
      case 'push': return 'success';
      case 'in-app': return 'warning';
      default: return 'default';
    }
  };

  const enabledCount = preferences.filter(p => p.enabled).length;

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Notification Settings</h1>
            <p className="text-muted-foreground">
              Manage system notification preferences for different events
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <Badge variant="outline">
              {enabledCount} of {preferences.length} enabled
            </Badge>
            <Button onClick={handleSave}>
              <Save className="h-4 w-4 mr-2" />
              Save Preferences
            </Button>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Notification Preferences</CardTitle>
            <CardDescription>
              Configure which notifications you want to receive and how you want to receive them
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {preferences.map((preference) => (
                <div
                  key={preference.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div className="flex items-center space-x-3">
                    <div className="flex items-center space-x-2">
                      {preference.icon}
                      <Badge variant={getTypeColor(preference.type) as any}>
                        {preference.type.toUpperCase()}
                      </Badge>
                    </div>
                    <div>
                      <p className="font-medium">{preference.label}</p>
                      <p className="text-sm text-muted-foreground">
                        {preference.description}
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={preference.enabled}
                    onCheckedChange={() => handleToggle(preference.id)}
                  />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Notification Channels</CardTitle>
              <CardDescription>
                Overview of notification delivery methods
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-sm">Email Notifications</span>
                  <Badge variant="default">
                    {preferences.filter(p => p.type === 'email' && p.enabled).length} active
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm">SMS Notifications</span>
                  <Badge variant="secondary">
                    {preferences.filter(p => p.type === 'sms' && p.enabled).length} active
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm">Push Notifications</span>
                  <Badge variant="success">
                    {preferences.filter(p => p.type === 'push' && p.enabled).length} active
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm">In-App Notifications</span>
                  <Badge variant="warning">
                    {preferences.filter(p => p.type === 'in-app' && p.enabled).length} active
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
              <CardDescription>
                Common notification management tasks
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Button 
                  variant="outline" 
                  className="w-full justify-start"
                  onClick={() => {
                    setPreferences(prev => prev.map(p => ({ ...p, enabled: true })));
                    toast.success('All notifications enabled');
                  }}
                >
                  <Bell className="h-4 w-4 mr-2" />
                  Enable All Notifications
                </Button>
                <Button 
                  variant="outline" 
                  className="w-full justify-start"
                  onClick={() => {
                    setPreferences(prev => prev.map(p => ({ ...p, enabled: false })));
                    toast.success('All notifications disabled');
                  }}
                >
                  <Bell className="h-4 w-4 mr-2" />
                  Disable All Notifications
                </Button>
                <Button 
                  variant="outline" 
                  className="w-full justify-start"
                  onClick={() => {
                    setPreferences(prev => prev.map(p => ({ 
                      ...p, 
                      enabled: p.type === 'email' || p.type === 'in-app' 
                    })));
                    toast.success('Reset to default preferences');
                  }}
                >
                  <MessageSquare className="h-4 w-4 mr-2" />
                  Reset to Defaults
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </MainLayout>
  );
}