'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from 'recharts';

// Mock data for payment volume over time
const paymentVolumeData = [
  { time: '00:00', volume: 1200, count: 45 },
  { time: '04:00', volume: 800, count: 32 },
  { time: '08:00', volume: 2100, count: 78 },
  { time: '12:00', volume: 3400, count: 125 },
  { time: '16:00', volume: 2800, count: 98 },
  { time: '20:00', volume: 1900, count: 67 },
];

// Mock data for success rate over time
const successRateData = [
  { time: '00:00', rate: 98.2 },
  { time: '04:00', rate: 99.1 },
  { time: '08:00', rate: 96.8 },
  { time: '12:00', rate: 97.5 },
  { time: '16:00', rate: 98.7 },
  { time: '20:00', rate: 97.9 },
];

export function PaymentVolumeChart() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Payment Volume</CardTitle>
        <CardDescription>
          Payment volume and count over the last 24 hours
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={paymentVolumeData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip 
              formatter={(value, name) => [
                name === 'volume' ? `$${value.toLocaleString()}` : value,
                name === 'volume' ? 'Volume' : 'Count'
              ]}
            />
            <Area
              type="monotone"
              dataKey="volume"
              stroke="#3b82f6"
              fill="#3b82f6"
              fillOpacity={0.6}
            />
          </AreaChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

export function SuccessRateChart() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Success Rate</CardTitle>
        <CardDescription>
          Payment success rate over the last 24 hours
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={successRateData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis domain={[95, 100]} />
            <Tooltip 
              formatter={(value) => [`${value}%`, 'Success Rate']}
            />
            <Line
              type="monotone"
              dataKey="rate"
              stroke="#10b981"
              strokeWidth={3}
              dot={{ fill: '#10b981' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}