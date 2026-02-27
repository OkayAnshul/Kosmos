import { TaskCard, TaskData } from './TaskCard';
import { TaskStatus } from './TaskStatusBadge';

interface KanbanColumnProps {
  title: string;
  status: TaskStatus;
  tasks: TaskData[];
  onTaskClick?: (taskId: string) => void;
}

export function KanbanColumn({ title, status, tasks, onTaskClick }: KanbanColumnProps) {
  return (
    <div className="flex-shrink-0 w-72">
      {/* Column Header */}
      <div className="bg-card border border-border rounded-xl p-3 mb-3" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
        <div className="flex items-center justify-between">
          <h3 style={{ fontWeight: '600', fontSize: '0.9375rem' }}>{title}</h3>
          <span
            className="px-2 py-0.5 bg-secondary rounded-full text-muted-foreground"
            style={{ fontSize: '0.75rem', fontWeight: '500' }}
          >
            {tasks.length}
          </span>
        </div>
      </div>

      {/* Tasks */}
      <div className="space-y-2 pb-4">
        {tasks.map((task) => (
          <TaskCard
            key={task.id}
            task={task}
            onClick={() => onTaskClick?.(task.id)}
            compact
          />
        ))}
        
        {tasks.length === 0 && (
          <div className="bg-card/50 border border-dashed border-border rounded-xl p-6 text-center">
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              No tasks
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
