import { MessageSquare, CheckSquare, Users, MoreVertical } from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import { MemberAvatars } from './MemberAvatars';
import { ProgressBar } from './ProgressBar';

export interface ProjectCardData {
  id: string;
  name: string;
  description: string;
  status: 'Active' | 'Archived';
  memberCount: number;
  chatCount: number;
  taskCount: number;
  completedTasks: number;
  lastActivity: string;
}

interface ProjectCardProps {
  project: ProjectCardData;
  onClick?: () => void;
}

export function ProjectCard({ project, onClick }: ProjectCardProps) {
  return (
    <div
      onClick={onClick}
      className="bg-card border border-border rounded-xl p-4 hover:shadow-lg hover:border-primary/20 transition-all cursor-pointer"
      style={{ boxShadow: '0 2px 8px rgba(0,0,0,0.3)' }}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-2">
        <div className="flex-1 min-w-0">
          <h3 className="mb-1 truncate" style={{ fontWeight: '600' }}>
            {project.name}
          </h3>
          <p
            className="text-muted-foreground line-clamp-2"
            style={{ fontSize: '0.875rem', lineHeight: '1.4' }}
          >
            {project.description}
          </p>
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation();
          }}
          className="ml-2 p-1 hover:bg-secondary rounded-lg transition-colors flex-shrink-0"
        >
          <MoreVertical size={18} className="text-muted-foreground" />
        </button>
      </div>

      {/* Status Badge */}
      <div className="mb-3">
        <StatusBadge status={project.status} />
      </div>

      {/* Stats Row */}
      <div className="flex items-center gap-4 mb-3">
        <div className="flex items-center gap-1.5">
          <Users size={16} className="text-muted-foreground" />
          <span className="text-foreground" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            {project.memberCount}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <MessageSquare size={16} className="text-muted-foreground" />
          <span className="text-foreground" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            {project.chatCount}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <CheckSquare size={16} className="text-muted-foreground" />
          <span className="text-foreground" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
            {project.taskCount}
          </span>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="mb-3">
        <ProgressBar value={project.completedTasks} max={project.taskCount} />
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <MemberAvatars count={project.memberCount} size="sm" />
        <span className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
          {project.lastActivity}
        </span>
      </div>
    </div>
  );
}