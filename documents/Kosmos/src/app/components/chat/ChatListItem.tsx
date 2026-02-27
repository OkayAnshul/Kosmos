import { Pin } from 'lucide-react';

export interface ChatListItemData {
  id: string;
  name: string;
  lastMessage: string;
  timestamp: string;
  unreadCount?: number;
  isPinned?: boolean;
  projectName: string;
}

interface ChatListItemProps {
  chat: ChatListItemData;
  onClick?: () => void;
}

export function ChatListItem({ chat, onClick }: ChatListItemProps) {
  return (
    <div
      onClick={onClick}
      className="bg-card border border-border rounded-xl p-4 hover:border-primary/20 transition-all cursor-pointer"
      style={{ boxShadow: '0 2px 6px rgba(0,0,0,0.2)' }}
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            {chat.isPinned && (
              <Pin size={14} className="text-primary flex-shrink-0" />
            )}
            <h3 className="truncate" style={{ fontWeight: '600', fontSize: '0.9375rem' }}>
              {chat.name}
            </h3>
          </div>
          <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
            {chat.projectName}
          </p>
        </div>
        
        <div className="flex flex-col items-end gap-2 flex-shrink-0">
          <span className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
            {chat.timestamp}
          </span>
          {chat.unreadCount && chat.unreadCount > 0 && (
            <span
              className="px-2 py-0.5 bg-primary text-primary-foreground rounded-full min-w-[20px] text-center"
              style={{ fontSize: '0.75rem', fontWeight: '600' }}
            >
              {chat.unreadCount}
            </span>
          )}
        </div>
      </div>
      
      <p
        className="text-muted-foreground line-clamp-2"
        style={{ fontSize: '0.875rem', lineHeight: '1.4' }}
      >
        {chat.lastMessage}
      </p>
    </div>
  );
}
