import { Calendar, MoreVertical } from 'lucide-react';
import { TaskStatusBadge, TaskStatus, getStatusColor } from './TaskStatusBadge';
import { PriorityBadge, TaskPriority } from './PriorityBadge';

export interface TaskData {
  id: string;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  assignee?: {
    name: string;
    avatar: string;
  };
  projectName?: string;
}

interface TaskCardProps {
  task: TaskData;
  onClick?: () => void;
  compact?: boolean;
}

export function TaskCard({ task, onClick, compact = false }: TaskCardProps) {
  const statusColor = getStatusColor(task.status);
  
  return (
    <div
      onClick={onClick}
      className="bg-card border border-border rounded-xl p-3 hover:border-primary/20 transition-all cursor-pointer relative overflow-hidden"
      style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}
    >
      {/* Status indicator bar */}
      <div
        className="absolute left-0 top-0 bottom-0 w-1"
        style={{ backgroundColor: statusColor }}
      />
      
      <div className="pl-2">
        {/* Header */}
        <div className="flex items-start justify-between gap-2 mb-2">
          <h4
            className="flex-1 line-clamp-2"
            style={{ fontWeight: '600', fontSize: compact ? '0.875rem' : '0.9375rem' }}
          >
            {task.title}
          </h4>
          <button
            onClick={(e) => {
              e.stopPropagation();
            }}
            className="p-1 hover:bg-secondary rounded transition-colors flex-shrink-0"
          >
            <MoreVertical size={16} className="text-muted-foreground" />
          </button>
        </div>

        {/* Project name (if exists) */}
        {task.projectName && !compact && (
          <p className="text-muted-foreground mb-2" style={{ fontSize: '0.75rem' }}>
            {task.projectName}
          </p>
        )}

        {/* Badges row */}
        <div className="flex items-center gap-2 mb-2 flex-wrap">
          <TaskStatusBadge status={task.status} />
          <PriorityBadge priority={task.priority} />
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-muted-foreground">
            {task.dueDate && (
              <>
                <Calendar size={14} />
                <span style={{ fontSize: '0.75rem' }}>{task.dueDate}</span>
              </>
            )}
          </div>
          
          {task.assignee && (
            <div
              className="w-6 h-6 rounded-full flex items-center justify-center text-white"
              style={{
                backgroundColor: '#7C3AED',
                fontSize: '0.625rem',
                fontWeight: '500',
                boxShadow: '0 2px 4px rgba(124, 58, 237, 0.4)'
              }}
            >
              {task.assignee.name[0]}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
