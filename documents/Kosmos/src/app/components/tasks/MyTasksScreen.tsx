import { useState } from 'react';
import { List, LayoutGrid, Filter, Plus } from 'lucide-react';
import { TaskCard, TaskData } from './TaskCard';
import { KanbanColumn } from './KanbanColumn';

const mockTasks: TaskData[] = [
  {
    id: '1',
    title: 'Design new onboarding flow for mobile app',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueDate: 'Jan 15',
    assignee: { name: 'Alice', avatar: 'A' },
    projectName: 'Mobile App Redesign'
  },
  {
    id: '2',
    title: 'Update API documentation',
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: 'Jan 18',
    assignee: { name: 'Bob', avatar: 'B' },
    projectName: 'API Documentation'
  },
  {
    id: '3',
    title: 'Fix login page responsiveness',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueDate: 'Jan 12',
    assignee: { name: 'Carol', avatar: 'C' },
    projectName: 'Customer Portal v2'
  },
  {
    id: '4',
    title: 'Review pull requests',
    status: 'TODO',
    priority: 'LOW',
    dueDate: 'Jan 16',
    assignee: { name: 'David', avatar: 'D' },
    projectName: 'Website Performance'
  },
  {
    id: '5',
    title: 'Write unit tests for auth module',
    status: 'DONE',
    priority: 'MEDIUM',
    dueDate: 'Jan 10',
    assignee: { name: 'Eve', avatar: 'E' },
    projectName: 'Customer Portal v2'
  },
  {
    id: '6',
    title: 'Setup CI/CD pipeline',
    status: 'DONE',
    priority: 'HIGH',
    dueDate: 'Jan 8',
    assignee: { name: 'Frank', avatar: 'F' },
    projectName: 'Website Performance'
  }
];

type ViewMode = 'list' | 'kanban';
type FilterType = 'all' | 'active' | 'completed';

interface MyTasksScreenProps {
  onTaskClick?: (taskId: string) => void;
}

export function MyTasksScreen({ onTaskClick }: MyTasksScreenProps) {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [filter, setFilter] = useState<FilterType>('all');

  const filteredTasks = mockTasks.filter((task) => {
    if (filter === 'active') return task.status !== 'DONE';
    if (filter === 'completed') return task.status === 'DONE';
    return true;
  });

  const todoTasks = filteredTasks.filter(t => t.status === 'TODO');
  const inProgressTasks = filteredTasks.filter(t => t.status === 'IN_PROGRESS');
  const doneTasks = filteredTasks.filter(t => t.status === 'DONE');

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3">
          <div className="flex items-center justify-between mb-3">
            <h1 style={{ fontWeight: '600', fontSize: '1.25rem' }}>My Tasks</h1>
            <div className="flex items-center gap-2">
              {/* View Toggle */}
              <div className="flex bg-secondary rounded-lg p-0.5">
                <button
                  onClick={() => setViewMode('list')}
                  className={`p-2 rounded transition-colors ${
                    viewMode === 'list' ? 'bg-card shadow-sm' : 'hover:bg-muted'
                  }`}
                >
                  <List size={18} className={viewMode === 'list' ? 'text-primary' : 'text-muted-foreground'} />
                </button>
                <button
                  onClick={() => setViewMode('kanban')}
                  className={`p-2 rounded transition-colors ${
                    viewMode === 'kanban' ? 'bg-card shadow-sm' : 'hover:bg-muted'
                  }`}
                >
                  <LayoutGrid size={18} className={viewMode === 'kanban' ? 'text-primary' : 'text-muted-foreground'} />
                </button>
              </div>
              
              <button className="p-2 hover:bg-secondary rounded-lg transition-colors">
                <Filter size={18} className="text-foreground" />
              </button>
            </div>
          </div>

          {/* Filter Chips */}
          <div className="flex gap-2 overflow-x-auto pb-1">
            {[
              { key: 'all', label: 'All' },
              { key: 'active', label: 'Active' },
              { key: 'completed', label: 'Completed' }
            ].map((item) => (
              <button
                key={item.key}
                onClick={() => setFilter(item.key as FilterType)}
                className={`px-3 py-1.5 rounded-lg transition-colors whitespace-nowrap ${
                  filter === item.key
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-secondary text-foreground hover:bg-muted'
                }`}
                style={{ fontSize: '0.875rem', fontWeight: '500' }}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Content */}
      {viewMode === 'list' ? (
        <div className="px-4 py-4">
          <div className="space-y-3">
            {filteredTasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onClick={() => onTaskClick?.(task.id)}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <div className="flex gap-3 px-4 py-4 min-w-min">
            <KanbanColumn
              title="To Do"
              status="TODO"
              tasks={todoTasks}
              onTaskClick={onTaskClick}
            />
            <KanbanColumn
              title="In Progress"
              status="IN_PROGRESS"
              tasks={inProgressTasks}
              onTaskClick={onTaskClick}
            />
            <KanbanColumn
              title="Done"
              status="DONE"
              tasks={doneTasks}
              onTaskClick={onTaskClick}
            />
          </div>
        </div>
      )}

      {/* FAB */}
      <button
        className="fixed bottom-6 right-6 w-14 h-14 bg-primary text-primary-foreground rounded-full flex items-center justify-center shadow-lg hover:opacity-90 transition-opacity"
        style={{ boxShadow: '0 4px 16px rgba(124, 58, 237, 0.5)' }}
      >
        <Plus size={24} />
      </button>
    </div>
  );
}
