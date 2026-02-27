import { ArrowLeft, Save, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { TaskStatus } from './TaskStatusBadge';
import { TaskPriority } from './PriorityBadge';

interface TaskEditScreenProps {
  taskId?: string;
  onBack?: () => void;
  onSave?: () => void;
}

export function TaskEditScreen({ taskId, onBack, onSave }: TaskEditScreenProps) {
  const isNewTask = !taskId;
  
  const [title, setTitle] = useState('Design new onboarding flow for mobile app');
  const [description, setDescription] = useState('Create a comprehensive onboarding experience that guides new users through the key features of the mobile application.');
  const [status, setStatus] = useState<TaskStatus>('IN_PROGRESS');
  const [priority, setPriority] = useState<TaskPriority>('HIGH');
  const [dueDate, setDueDate] = useState('2026-01-15');

  const handleSave = () => {
    onSave?.();
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={onBack}
              className="p-2 hover:bg-secondary rounded-lg transition-colors"
            >
              <ArrowLeft size={22} className="text-foreground" />
            </button>
            <h1 style={{ fontWeight: '600', fontSize: '1.125rem' }}>
              {isNewTask ? 'New Task' : 'Edit Task'}
            </h1>
          </div>
          <button
            onClick={handleSave}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
            style={{ fontSize: '0.875rem', fontWeight: '500' }}
          >
            <Save size={18} />
            Save
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="px-4 py-4 space-y-4">
        {/* Title */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Title
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-4 py-3 bg-card border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
            style={{ fontSize: '0.9375rem' }}
            placeholder="Enter task title"
          />
        </div>

        {/* Description */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            className="w-full px-4 py-3 bg-card border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none"
            style={{ fontSize: '0.9375rem' }}
            placeholder="Enter task description"
          />
        </div>

        {/* Status */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Status
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['TODO', 'IN_PROGRESS', 'DONE'] as TaskStatus[]).map((s) => (
              <button
                key={s}
                onClick={() => setStatus(s)}
                className={`px-4 py-3 rounded-xl border transition-colors ${
                  status === s
                    ? 'bg-primary border-primary text-primary-foreground'
                    : 'bg-card border-border text-foreground hover:border-primary/30'
                }`}
                style={{ fontSize: '0.875rem', fontWeight: '500' }}
              >
                {s === 'TODO' ? 'To Do' : s === 'IN_PROGRESS' ? 'In Progress' : 'Done'}
              </button>
            ))}
          </div>
        </div>

        {/* Priority */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Priority
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['LOW', 'MEDIUM', 'HIGH'] as TaskPriority[]).map((p) => (
              <button
                key={p}
                onClick={() => setPriority(p)}
                className={`px-4 py-3 rounded-xl border transition-colors ${
                  priority === p
                    ? 'bg-primary border-primary text-primary-foreground'
                    : 'bg-card border-border text-foreground hover:border-primary/30'
                }`}
                style={{ fontSize: '0.875rem', fontWeight: '500' }}
              >
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>

        {/* Due Date */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Due Date
          </label>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className="w-full px-4 py-3 bg-card border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
            style={{ fontSize: '0.9375rem' }}
          />
        </div>

        {/* Project */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Project
          </label>
          <select
            className="w-full px-4 py-3 bg-card border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
            style={{ fontSize: '0.9375rem' }}
          >
            <option>Mobile App Redesign</option>
            <option>Marketing Campaign Q1</option>
            <option>Customer Portal v2</option>
            <option>Website Performance</option>
          </select>
        </div>

        {/* Assignee */}
        <div>
          <label className="block text-muted-foreground mb-2" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            Assignee
          </label>
          <select
            className="w-full px-4 py-3 bg-card border border-border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
            style={{ fontSize: '0.9375rem' }}
          >
            <option>Alice Chen</option>
            <option>Bob Smith</option>
            <option>Carol Davis</option>
            <option>David Lee</option>
          </select>
        </div>

        {/* Delete Button */}
        {!isNewTask && (
          <button
            className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-destructive/10 text-destructive border border-destructive/30 rounded-xl hover:bg-destructive/20 transition-colors"
            style={{ fontSize: '0.875rem', fontWeight: '500' }}
          >
            <Trash2 size={18} />
            Delete Task
          </button>
        )}
      </div>
    </div>
  );
}
