import { ArrowLeft, Edit2, Calendar, User, Clock, CheckSquare, MessageSquare } from 'lucide-react';
import { TaskStatusBadge, TaskStatus } from './TaskStatusBadge';
import { PriorityBadge, TaskPriority } from './PriorityBadge';

interface TaskDetailScreenProps {
  taskId: string;
  onBack?: () => void;
  onEdit?: () => void;
}

export function TaskDetailScreen({ taskId, onBack, onEdit }: TaskDetailScreenProps) {
  // Mock data
  const task = {
    id: taskId,
    title: 'Design new onboarding flow for mobile app',
    status: 'IN_PROGRESS' as TaskStatus,
    priority: 'HIGH' as TaskPriority,
    description: 'Create a comprehensive onboarding experience that guides new users through the key features of the mobile application. Focus on clarity and engagement.',
    dueDate: 'Jan 15, 2026',
    assignee: { name: 'Alice Chen', avatar: 'A' },
    projectName: 'Mobile App Redesign',
    createdAt: 'Jan 8, 2026',
    subtasks: [
      { id: '1', title: 'Design welcome screen', completed: true },
      { id: '2', title: 'Create feature tour screens', completed: true },
      { id: '3', title: 'Design skip/continue interactions', completed: false },
      { id: '4', title: 'Add animations and transitions', completed: false }
    ],
    timeTracked: '4h 30m',
    timeEstimate: '8h',
    activity: [
      { id: '1', user: 'Alice', action: 'changed status to In Progress', time: '2 hours ago' },
      { id: '2', user: 'Bob', action: 'added a comment', time: '5 hours ago' },
      { id: '3', user: 'Alice', action: 'completed subtask "Design welcome screen"', time: '1 day ago' }
    ]
  };

  const completedSubtasks = task.subtasks.filter(st => st.completed).length;

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <button
            onClick={onBack}
            className="p-2 hover:bg-secondary rounded-lg transition-colors"
          >
            <ArrowLeft size={22} className="text-foreground" />
          </button>
          <button
            onClick={onEdit}
            className="p-2 hover:bg-secondary rounded-lg transition-colors"
          >
            <Edit2 size={20} className="text-foreground" />
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="px-4 py-4 space-y-4">
        {/* Title Section */}
        <div>
          <p className="text-muted-foreground mb-2" style={{ fontSize: '0.875rem' }}>
            {task.projectName}
          </p>
          <h1 className="mb-3" style={{ fontWeight: '600', fontSize: '1.5rem', lineHeight: '1.3' }}>
            {task.title}
          </h1>
          <div className="flex items-center gap-2 flex-wrap">
            <TaskStatusBadge status={task.status} />
            <PriorityBadge priority={task.priority} showLabel />
          </div>
        </div>

        {/* Meta Info */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-card border border-border rounded-xl p-3" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <Calendar size={16} />
              <span style={{ fontSize: '0.75rem' }}>Due Date</span>
            </div>
            <p style={{ fontSize: '0.9375rem', fontWeight: '500' }}>{task.dueDate}</p>
          </div>
          
          <div className="bg-card border border-border rounded-xl p-3" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <User size={16} />
              <span style={{ fontSize: '0.75rem' }}>Assignee</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className="w-5 h-5 rounded-full flex items-center justify-center text-white"
                style={{
                  backgroundColor: '#7C3AED',
                  fontSize: '0.625rem',
                  fontWeight: '500'
                }}
              >
                {task.assignee.avatar}
              </div>
              <p style={{ fontSize: '0.9375rem', fontWeight: '500' }}>{task.assignee.name}</p>
            </div>
          </div>
        </div>

        {/* Description */}
        <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
          <h3 className="mb-2" style={{ fontWeight: '600' }}>Description</h3>
          <p className="text-muted-foreground" style={{ fontSize: '0.875rem', lineHeight: '1.6' }}>
            {task.description}
          </p>
        </div>

        {/* Subtasks */}
        <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
          <div className="flex items-center justify-between mb-3">
            <h3 style={{ fontWeight: '600' }}>Subtasks</h3>
            <span className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              {completedSubtasks}/{task.subtasks.length}
            </span>
          </div>
          <div className="space-y-2">
            {task.subtasks.map((subtask) => (
              <div key={subtask.id} className="flex items-center gap-3 p-2 hover:bg-secondary rounded-lg transition-colors">
                <div
                  className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 ${
                    subtask.completed
                      ? 'bg-primary border-primary'
                      : 'border-muted-foreground'
                  }`}
                >
                  {subtask.completed && (
                    <CheckSquare size={14} className="text-primary-foreground" />
                  )}
                </div>
                <span
                  className={subtask.completed ? 'text-muted-foreground line-through' : 'text-foreground'}
                  style={{ fontSize: '0.875rem' }}
                >
                  {subtask.title}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Time Tracking */}
        <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
          <h3 className="mb-3" style={{ fontWeight: '600' }}>Time Tracking</h3>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-primary" />
              <div>
                <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>Tracked</p>
                <p style={{ fontSize: '0.9375rem', fontWeight: '500' }}>{task.timeTracked}</p>
              </div>
            </div>
            <div className="h-8 w-px bg-border" />
            <div>
              <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>Estimate</p>
              <p style={{ fontSize: '0.9375rem', fontWeight: '500' }}>{task.timeEstimate}</p>
            </div>
          </div>
        </div>

        {/* Activity Timeline */}
        <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
          <h3 className="mb-4" style={{ fontWeight: '600' }}>Activity</h3>
          <div className="space-y-4">
            {task.activity.map((item, index) => (
              <div key={item.id} className="flex gap-3">
                <div className="relative">
                  <div
                    className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                    style={{
                      backgroundColor: '#7C3AED',
                      color: 'white',
                      fontSize: '0.75rem',
                      fontWeight: '500'
                    }}
                  >
                    {item.user[0]}
                  </div>
                  {index < task.activity.length - 1 && (
                    <div className="absolute top-8 left-1/2 -translate-x-1/2 w-px h-4 bg-border" />
                  )}
                </div>
                <div className="flex-1 pt-1">
                  <p style={{ fontSize: '0.875rem' }}>
                    <span style={{ fontWeight: '600' }}>{item.user}</span>{' '}
                    <span className="text-muted-foreground">{item.action}</span>
                  </p>
                  <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
                    {item.time}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Comments Section */}
        <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
          <div className="flex items-center gap-2 mb-4">
            <MessageSquare size={18} className="text-muted-foreground" />
            <h3 style={{ fontWeight: '600' }}>Comments</h3>
          </div>
          <div className="flex gap-3">
            <div
              className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
              style={{
                backgroundColor: '#6366F1',
                color: 'white',
                fontSize: '0.75rem',
                fontWeight: '500'
              }}
            >
              Y
            </div>
            <input
              type="text"
              placeholder="Add a comment..."
              className="flex-1 px-3 py-2 bg-secondary border-0 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
              style={{ fontSize: '0.875rem' }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
