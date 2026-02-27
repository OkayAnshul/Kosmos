export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

interface TaskStatusBadgeProps {
  status: TaskStatus;
}

const statusConfig = {
  TODO: {
    label: 'To Do',
    bg: 'bg-muted',
    text: 'text-muted-foreground',
    border: 'border-muted',
    color: '#6B7280'
  },
  IN_PROGRESS: {
    label: 'In Progress',
    bg: 'bg-primary/20',
    text: 'text-primary',
    border: 'border-primary/30',
    color: '#7C3AED'
  },
  DONE: {
    label: 'Done',
    bg: 'bg-emerald-500/20',
    text: 'text-emerald-400',
    border: 'border-emerald-500/30',
    color: '#10B981'
  }
};

export function TaskStatusBadge({ status }: TaskStatusBadgeProps) {
  const config = statusConfig[status];
  
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full border ${config.bg} ${config.text} ${config.border}`}
      style={{ fontSize: '0.75rem', fontWeight: '500' }}
    >
      {config.label}
    </span>
  );
}

export function getStatusColor(status: TaskStatus): string {
  return statusConfig[status].color;
}
