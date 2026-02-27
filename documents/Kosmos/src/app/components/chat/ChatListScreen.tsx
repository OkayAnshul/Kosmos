import { useState } from 'react';
import { Search, Plus } from 'lucide-react';
import { ChatListItem, ChatListItemData } from './ChatListItem';

const mockChats: ChatListItemData[] = [
  {
    id: '1',
    name: 'Design Discussion',
    lastMessage: 'Alice: I think we should use the purple variant for the primary buttons',
    timestamp: '2m ago',
    unreadCount: 3,
    isPinned: true,
    projectName: 'Mobile App Redesign'
  },
  {
    id: '2',
    name: 'Sprint Planning',
    lastMessage: 'Bob: Let\'s schedule the planning meeting for tomorrow at 10 AM',
    timestamp: '1h ago',
    unreadCount: 1,
    isPinned: false,
    projectName: 'Customer Portal v2'
  },
  {
    id: '3',
    name: 'Bug Fixes',
    lastMessage: 'Carol: Fixed the login issue on Safari',
    timestamp: '3h ago',
    isPinned: false,
    projectName: 'Customer Portal v2'
  },
  {
    id: '4',
    name: 'Marketing Assets',
    lastMessage: 'David: Updated the campaign images, please review',
    timestamp: '1d ago',
    unreadCount: 5,
    isPinned: false,
    projectName: 'Marketing Campaign Q1'
  },
  {
    id: '5',
    name: 'Performance Review',
    lastMessage: 'Eve: The new caching strategy improved load times by 40%',
    timestamp: '2d ago',
    isPinned: false,
    projectName: 'Website Performance'
  }
];

type FilterType = 'all' | 'unread' | 'mentions';

interface ChatListScreenProps {
  onChatClick?: (chatId: string) => void;
}

export function ChatListScreen({ onChatClick }: ChatListScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState<FilterType>('all');

  const filteredChats = mockChats.filter((chat) => {
    const matchesSearch = chat.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         chat.lastMessage.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = 
      filter === 'all' ||
      (filter === 'unread' && chat.unreadCount && chat.unreadCount > 0);
    
    return matchesSearch && matchesFilter;
  });

  // Sort: pinned first, then by unread, then by timestamp
  const sortedChats = [...filteredChats].sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1;
    if (!a.isPinned && b.isPinned) return 1;
    if ((a.unreadCount || 0) > 0 && (b.unreadCount || 0) === 0) return -1;
    if ((a.unreadCount || 0) === 0 && (b.unreadCount || 0) > 0) return 1;
    return 0;
  });

  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border sticky top-0 z-10">
        <div className="px-4 py-3">
          <h1 className="mb-3" style={{ fontWeight: '600', fontSize: '1.25rem' }}>
            Chats
          </h1>

          {/* Search Bar */}
          <div className="mb-3">
            <div className="relative">
              <Search
                size={20}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
              />
              <input
                type="text"
                placeholder="Search chats..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-secondary border-0 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20"
                style={{ fontSize: '0.9375rem' }}
              />
            </div>
          </div>

          {/* Filter Chips */}
          <div className="flex gap-2 overflow-x-auto pb-1">
            {[
              { key: 'all', label: 'All' },
              { key: 'unread', label: 'Unread' },
              { key: 'mentions', label: 'Mentions' }
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

      {/* Chat List */}
      <div className="px-4 py-4 space-y-3">
        {sortedChats.map((chat) => (
          <ChatListItem
            key={chat.id}
            chat={chat}
            onClick={() => onChatClick?.(chat.id)}
          />
        ))}

        {sortedChats.length === 0 && (
          <div className="text-center py-16">
            <p className="text-muted-foreground" style={{ fontSize: '0.875rem' }}>
              No chats found
            </p>
          </div>
        )}
      </div>

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
