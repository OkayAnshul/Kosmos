import { Home, CheckSquare, MessageSquare, User } from 'lucide-react';

export type NavTab = 'projects' | 'tasks' | 'chats' | 'profile';

interface BottomNavigationProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

export function BottomNavigation({ activeTab, onTabChange }: BottomNavigationProps) {
  const tabs = [
    { key: 'projects' as NavTab, label: 'Projects', icon: Home },
    { key: 'tasks' as NavTab, label: 'Tasks', icon: CheckSquare },
    { key: 'chats' as NavTab, label: 'Chats', icon: MessageSquare },
    { key: 'profile' as NavTab, label: 'Profile', icon: User }
  ];

  return (
    <div className="bg-card border-t border-border">
      <div className="flex items-center justify-around px-2 py-2">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          
          return (
            <button
              key={tab.key}
              onClick={() => onTabChange(tab.key)}
              className={`flex flex-col items-center gap-1 px-4 py-2 rounded-lg transition-colors ${
                isActive ? 'text-primary' : 'text-muted-foreground'
              }`}
            >
              <Icon size={22} />
              <span style={{ fontSize: '0.75rem', fontWeight: isActive ? '600' : '500' }}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
