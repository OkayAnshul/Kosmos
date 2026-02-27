import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  icon: LucideIcon;
  label: string;
  value: number;
  color?: string;
}

export function StatCard({ icon: Icon, label, value, color = '#7C3AED' }: StatCardProps) {
  return (
    <div 
      className="bg-card border border-border rounded-xl p-4 flex items-start gap-3"
      style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}
    >
      <div
        className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
        style={{ 
          backgroundColor: `${color}20`,
          boxShadow: `0 0 12px ${color}15`
        }}
      >
        <Icon size={20} style={{ color }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
          {label}
        </p>
        <p className="mt-1" style={{ fontSize: '1.5rem', fontWeight: '600', lineHeight: '1' }}>
          {value}
        </p>
      </div>
    </div>
  );
}