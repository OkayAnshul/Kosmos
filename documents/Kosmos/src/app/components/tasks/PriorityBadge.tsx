import { AlertCircle, ArrowUp, Minus } from 'lucide-react';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

interface PriorityBadgeProps {
  priority: TaskPriority;
  showLabel?: boolean;
}

const priorityConfig = {
  LOW: {
    label: 'Low',
    icon: Minus,
    color: '#6B7280',
    bg: '#6B728015'
  },
  MEDIUM: {
    label: 'Medium',
    icon: AlertCircle,
    color: '#F59E0B',
    bg: '#F59E0B15'
  },
  HIGH: {
    label: 'High',
    icon: ArrowUp,
    color: '#EF4444',
    bg: '#EF444415'
  }
};

export function PriorityBadge({ priority, showLabel = false }: PriorityBadgeProps) {
  const config = priorityConfig[priority];
  const Icon = config.icon;
  
  return (
    <span
      className="inline-flex items-center gap-1 px-2 py-0.5 rounded"
      style={{
        backgroundColor: config.bg,
        color: config.color,
        fontSize: '0.75rem',
        fontWeight: '500'
      }}
    >
      <Icon size={12} />
      {showLabel && config.label}
    </span>
  );
}

export function getPriorityColor(priority: TaskPriority): string {
  return priorityConfig[priority].color;
}
