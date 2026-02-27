import { useState } from 'react';
import { BottomNavigation, NavTab } from './components/BottomNavigation';

// Projects
import { ProjectListScreen } from './components/ProjectListScreen';
import { ProjectDetailsScreen } from './components/ProjectDetailsScreen';

// Tasks
import { MyTasksScreen } from './components/tasks/MyTasksScreen';
import { TaskDetailScreen } from './components/tasks/TaskDetailScreen';
import { TaskEditScreen } from './components/tasks/TaskEditScreen';

// Chats
import { ChatListScreen } from './components/chat/ChatListScreen';
import { ChatRoomScreen } from './components/chat/ChatRoomScreen';

type AppView = 
  | { type: 'tab'; tab: NavTab }
  | { type: 'projectDetail'; projectId: string }
  | { type: 'taskDetail'; taskId: string }
  | { type: 'taskEdit'; taskId?: string }
  | { type: 'chatRoom'; chatId: string };

export default function App() {
  const [view, setView] = useState<AppView>({ type: 'tab', tab: 'projects' });
  const [currentTab, setCurrentTab] = useState<NavTab>('projects');

  const handleTabChange = (tab: NavTab) => {
    setCurrentTab(tab);
    setView({ type: 'tab', tab });
  };

  const handleProjectClick = (projectId: string) => {
    setView({ type: 'projectDetail', projectId });
  };

  const handleTaskClick = (taskId: string) => {
    setView({ type: 'taskDetail', taskId });
  };

  const handleChatClick = (chatId: string) => {
    setView({ type: 'chatRoom', chatId });
  };

  const handleBack = () => {
    setView({ type: 'tab', tab: currentTab });
  };

  const handleEditTask = (taskId?: string) => {
    setView({ type: 'taskEdit', taskId });
  };

  const renderContent = () => {
    switch (view.type) {
      case 'tab':
        switch (view.tab) {
          case 'projects':
            return <ProjectListScreen onProjectClick={handleProjectClick} />;
          case 'tasks':
            return <MyTasksScreen onTaskClick={handleTaskClick} />;
          case 'chats':
            return <ChatListScreen onChatClick={handleChatClick} />;
          case 'profile':
            return (
              <div className="min-h-screen bg-background flex items-center justify-center">
                <div className="text-center">
                  <div className="w-20 h-20 bg-primary rounded-full flex items-center justify-center mx-auto mb-4">
                    <span className="text-primary-foreground" style={{ fontSize: '2rem', fontWeight: '600' }}>
                      Y
                    </span>
                  </div>
                  <h2 className="mb-2" style={{ fontWeight: '600', fontSize: '1.25rem' }}>
                    Your Profile
                  </h2>
                  <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
                    Profile screen coming soon
                  </p>
                </div>
              </div>
            );
        }
        break;
      case 'projectDetail':
        return <ProjectDetailsScreen projectId={view.projectId} onBack={handleBack} />;
      case 'taskDetail':
        return (
          <TaskDetailScreen
            taskId={view.taskId}
            onBack={handleBack}
            onEdit={() => handleEditTask(view.taskId)}
          />
        );
      case 'taskEdit':
        return <TaskEditScreen taskId={view.taskId} onBack={handleBack} onSave={handleBack} />;
      case 'chatRoom':
        return <ChatRoomScreen chatId={view.chatId} onBack={handleBack} />;
    }
  };

  const showBottomNav = view.type === 'tab';

  return (
    <div className="max-w-md mx-auto bg-background min-h-screen flex flex-col">
      <div className="flex-1 overflow-auto">
        {renderContent()}
      </div>
      {showBottomNav && (
        <BottomNavigation activeTab={currentTab} onTabChange={handleTabChange} />
      )}
    </div>
  );
}
