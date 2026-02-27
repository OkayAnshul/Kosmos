import { useState } from 'react';
import { ArrowLeft, MoreVertical, MessageSquare, CheckSquare, Users, Activity, Plus } from 'lucide-react';
import { StatCard } from './StatCard';
import { MemberAvatars } from './MemberAvatars';
import { StatusBadge } from './StatusBadge';

type TabType = 'Overview' | 'Chats' | 'Tasks' | 'Members' | 'Activity';

interface ProjectDetailsScreenProps {
  projectId: string;
  onBack?: () => void;
}

export function ProjectDetailsScreen({ projectId, onBack }: ProjectDetailsScreenProps) {
  const [activeTab, setActiveTab] = useState<TabType>('Overview');

  // Mock project data
  const project = {
    id: projectId,
    name: 'Mobile App Redesign',
    description: 'Complete redesign of the mobile application with new branding guidelines, improved user experience, and modern UI components. This project aims to increase user engagement and satisfaction.',
    status: 'Active' as const,
    memberCount: 8,
    chatCount: 12,
    taskCount: 24,
    completedTasks: 16,
    lastActivity: '2 hours ago'
  };

  const tabs: TabType[] = ['Overview', 'Chats', 'Tasks', 'Members', 'Activity'];

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <button
              onClick={onBack}
              className="p-2 hover:bg-secondary rounded-lg transition-colors flex-shrink-0"
            >
              <ArrowLeft size={22} className="text-foreground" />
            </button>
            <h1 className="truncate" style={{ fontWeight: '600', fontSize: '1.125rem' }}>
              {project.name}
            </h1>
          </div>
          <button className="p-2 hover:bg-secondary rounded-lg transition-colors flex-shrink-0">
            <MoreVertical size={22} className="text-foreground" />
          </button>
        </div>

        {/* Tabs */}
        <div className="overflow-x-auto">
          <div className="flex px-4 gap-1">
            {tabs.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-3 whitespace-nowrap relative transition-colors ${
                  activeTab === tab
                    ? 'text-primary'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
                style={{ fontSize: '0.875rem', fontWeight: '500' }}
              >
                {tab}
                {activeTab === tab && (
                  <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
                )}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="px-4 py-4">
        {activeTab === 'Overview' && (
          <div className="space-y-4">
            {/* Project Info Card */}
            <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}>
              <div className="flex items-start justify-between mb-3">
                <h3 style={{ fontWeight: '600' }}>Project Details</h3>
                <StatusBadge status={project.status} />
              </div>
              <p className="text-muted-foreground" style={{ fontSize: '0.875rem', lineHeight: '1.6' }}>
                {project.description}
              </p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 gap-3">
              <StatCard
                icon={MessageSquare}
                label="Active Chats"
                value={project.chatCount}
                color="#7C3AED"
              />
              <StatCard
                icon={CheckSquare}
                label="Total Tasks"
                value={project.taskCount}
                color="#A855F7"
              />
              <StatCard
                icon={Users}
                label="Team Members"
                value={project.memberCount}
                color="#6366F1"
              />
            </div>

            {/* Quick Actions */}
            <div>
              <h3 className="mb-3" style={{ fontWeight: '600' }}>Quick Actions</h3>
              <div className="grid grid-cols-2 gap-3">
                <button className="flex items-center justify-center gap-2 px-4 py-3 bg-primary text-primary-foreground rounded-xl hover:opacity-90 transition-opacity shadow-lg" style={{ boxShadow: '0 4px 12px rgba(124, 58, 237, 0.4)' }}>
                  <Plus size={20} />
                  <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>New Task</span>
                </button>
                <button className="flex items-center justify-center gap-2 px-4 py-3 bg-secondary text-foreground rounded-xl hover:bg-muted transition-colors border border-border">
                  <Plus size={20} />
                  <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>New Chat</span>
                </button>
              </div>
            </div>

            {/* Team Members Preview */}
            <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 style={{ fontWeight: '600' }}>Team Members</h3>
                <button className="text-primary" style={{ fontSize: '0.875rem', fontWeight: '500' }}>
                  View All
                </button>
              </div>
              <div className="flex items-center gap-3">
                <MemberAvatars count={project.memberCount} maxDisplay={6} size="md" />
                <span className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
                  {project.memberCount} members
                </span>
              </div>
            </div>

            {/* Recent Activity */}
            <div className="bg-card border border-border rounded-xl p-4" style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}>
              <h3 className="mb-4" style={{ fontWeight: '600' }}>Recent Activity</h3>
              <div className="space-y-3">
                {[
                  { user: 'Alice', action: 'completed task "Design mockups"', time: '2 hours ago' },
                  { user: 'Bob', action: 'added 3 new tasks', time: '3 hours ago' },
                  { user: 'Carol', action: 'started a new chat', time: '5 hours ago' }
                ].map((activity, i) => (
                  <div key={i} className="flex items-start gap-3">
                    <div
                      className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                      style={{
                        backgroundColor: '#7C3AED',
                        color: 'white',
                        fontSize: '0.75rem',
                        fontWeight: '500',
                        boxShadow: '0 2px 4px rgba(124, 58, 237, 0.4)'
                      }}
                    >
                      {activity.user[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p style={{ fontSize: '0.875rem' }}>
                        <span style={{ fontWeight: '600' }}>{activity.user}</span>{' '}
                        <span className="text-muted-foreground">{activity.action}</span>
                      </p>
                      <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
                        {activity.time}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'Chats' && (
          <div className="text-center py-16">
            <div className="w-16 h-16 bg-secondary rounded-full flex items-center justify-center mx-auto mb-4">
              <MessageSquare size={32} className="text-muted-foreground" />
            </div>
            <h3 className="mb-2" style={{ fontWeight: '600' }}>Chats</h3>
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              Chat functionality coming soon
            </p>
          </div>
        )}

        {activeTab === 'Tasks' && (
          <div className="text-center py-16">
            <div className="w-16 h-16 bg-secondary rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckSquare size={32} className="text-muted-foreground" />
            </div>
            <h3 className="mb-2" style={{ fontWeight: '600' }}>Tasks</h3>
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              Task management interface coming soon
            </p>
          </div>
        )}

        {activeTab === 'Members' && (
          <div className="text-center py-16">
            <div className="w-16 h-16 bg-secondary rounded-full flex items-center justify-center mx-auto mb-4">
              <Users size={32} className="text-muted-foreground" />
            </div>
            <h3 className="mb-2" style={{ fontWeight: '600' }}>Members</h3>
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              Member management coming soon
            </p>
          </div>
        )}

        {activeTab === 'Activity' && (
          <div className="text-center py-16">
            <div className="w-16 h-16 bg-secondary rounded-full flex items-center justify-center mx-auto mb-4">
              <Activity size={32} className="text-muted-foreground" />
            </div>
            <h3 className="mb-2" style={{ fontWeight: '600' }}>Activity</h3>
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              Full activity timeline coming soon
            </p>
          </div>
        )}
      </div>
    </div>
  );
}