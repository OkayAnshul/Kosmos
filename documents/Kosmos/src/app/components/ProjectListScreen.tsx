import { useState } from 'react';
import { Search, Bell, Inbox } from 'lucide-react';
import { ProjectCard, ProjectCardData } from './ProjectCard';

const mockProjects: ProjectCardData[] = [
  {
    id: '1',
    name: 'Mobile App Redesign',
    description: 'Complete redesign of the mobile application with new branding and improved user experience',
    status: 'Active',
    memberCount: 8,
    chatCount: 12,
    taskCount: 24,
    completedTasks: 16,
    lastActivity: '2 hours ago'
  },
  {
    id: '2',
    name: 'Marketing Campaign Q1',
    description: 'Launch the new product marketing campaign across all channels',
    status: 'Active',
    memberCount: 5,
    chatCount: 8,
    taskCount: 18,
    completedTasks: 12,
    lastActivity: '3 hours ago'
  },
  {
    id: '3',
    name: 'Website Performance',
    description: 'Optimize website loading times and improve Core Web Vitals scores',
    status: 'Active',
    memberCount: 4,
    chatCount: 5,
    taskCount: 15,
    completedTasks: 15,
    lastActivity: '1 day ago'
  },
  {
    id: '4',
    name: 'Customer Portal v2',
    description: 'Build the next generation customer self-service portal',
    status: 'Active',
    memberCount: 10,
    chatCount: 15,
    taskCount: 32,
    completedTasks: 8,
    lastActivity: '5 hours ago'
  },
  {
    id: '5',
    name: 'API Documentation',
    description: 'Create comprehensive API documentation for external developers',
    status: 'Archived',
    memberCount: 3,
    chatCount: 4,
    taskCount: 10,
    completedTasks: 10,
    lastActivity: '2 weeks ago'
  }
];

interface ProjectListScreenProps {
  onProjectClick?: (projectId: string) => void;
}

export function ProjectListScreen({ onProjectClick }: ProjectListScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<'All' | 'Active' | 'Archived'>('All');

  const filteredProjects = mockProjects.filter((project) => {
    const matchesSearch = project.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         project.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = activeFilter === 'All' || project.status === activeFilter;
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <h1 style={{ fontWeight: '600', fontSize: '1.25rem' }}>Projects</h1>
          <button className="p-2 hover:bg-secondary rounded-lg transition-colors">
            <Bell size={22} className="text-foreground" />
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="px-4 py-4">
        {/* Search Bar */}
        <div className="mb-4">
          <div className="relative">
            <Search
              size={20}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <input
              type="text"
              placeholder="Search projects..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-3 bg-secondary border-0 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
              style={{ fontSize: '0.9375rem' }}
            />
          </div>
        </div>

        {/* Filter Chips */}
        <div className="flex gap-2 mb-4 overflow-x-auto pb-1">
          {(['All', 'Active', 'Archived'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`px-4 py-2 rounded-lg transition-colors whitespace-nowrap ${
                activeFilter === filter
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-secondary text-foreground hover:bg-muted'
              }`}
              style={{ fontSize: '0.875rem', fontWeight: '500' }}
            >
              {filter}
            </button>
          ))}
        </div>

        {/* Project List */}
        {filteredProjects.length > 0 ? (
          <div className="space-y-3">
            {filteredProjects.map((project) => (
              <ProjectCard
                key={project.id}
                project={project}
                onClick={() => onProjectClick?.(project.id)}
              />
            ))}
          </div>
        ) : (
          // Empty State
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-16 h-16 bg-secondary rounded-full flex items-center justify-center mb-4">
              <Inbox size={32} className="text-muted-foreground" />
            </div>
            <h3 className="mb-2" style={{ fontWeight: '600' }}>
              No projects found
            </h3>
            <p className="text-muted-foreground text-center" style={{ fontSize: '0.875rem' }}>
              {searchQuery
                ? 'Try adjusting your search or filters'
                : 'Create your first project to get started'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
