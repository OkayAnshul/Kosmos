import { CheckSquare } from 'lucide-react';

export type MessageType = 'user' | 'system' | 'task';

export interface MessageData {
  id: string;
  type: MessageType;
  sender?: {
    name: string;
    avatar: string;
  };
  content: string;
  timestamp: string;
  isOwn?: boolean;
  taskLink?: {
    title: string;
    status: string;
  };
  reactions?: {
    emoji: string;
    count: number;
  }[];
}

interface MessageBubbleProps {
  message: MessageData;
  showAvatar?: boolean;
  showName?: boolean;
}

export function MessageBubble({ message, showAvatar = true, showName = true }: MessageBubbleProps) {
  // System message
  if (message.type === 'system') {
    return (
      <div className="flex justify-center my-3">
        <div className="px-3 py-1.5 bg-secondary rounded-full">
          <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
            {message.content}
          </p>
        </div>
      </div>
    );
  }

  // Task-linked message
  if (message.type === 'task' && message.taskLink) {
    return (
      <div className="flex gap-3 mb-3">
        {showAvatar ? (
          <div
            className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 text-white"
            style={{
              backgroundColor: '#7C3AED',
              fontSize: '0.75rem',
              fontWeight: '500'
            }}
          >
            {message.sender?.avatar}
          </div>
        ) : (
          <div className="w-8" />
        )}
        
        <div className="flex-1 min-w-0">
          {showName && message.sender && (
            <p className="mb-1" style={{ fontSize: '0.875rem', fontWeight: '600' }}>
              {message.sender.name}
            </p>
          )}
          
          <div className="bg-primary/10 border border-primary/30 rounded-xl p-3 mb-1">
            <div className="flex items-start gap-2 mb-2">
              <CheckSquare size={16} className="text-primary mt-0.5 flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-primary" style={{ fontSize: '0.875rem', fontWeight: '600' }}>
                  {message.taskLink.title}
                </p>
                <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
                  {message.taskLink.status}
                </p>
              </div>
            </div>
            <p style={{ fontSize: '0.875rem', lineHeight: '1.5' }}>
              {message.content}
            </p>
          </div>
          
          <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
            {message.timestamp}
          </p>
        </div>
      </div>
    );
  }

  // Regular user message
  return (
    <div className="flex gap-3 mb-3">
      {showAvatar ? (
        <div
          className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 text-white"
          style={{
            backgroundColor: message.isOwn ? '#6366F1' : '#7C3AED',
            fontSize: '0.75rem',
            fontWeight: '500'
          }}
        >
          {message.sender?.avatar}
        </div>
      ) : (
        <div className="w-8" />
      )}
      
      <div className="flex-1 min-w-0">
        {showName && message.sender && (
          <p className="mb-1" style={{ fontSize: '0.875rem', fontWeight: '600' }}>
            {message.sender.name}
          </p>
        )}
        
        <div className="bg-card border border-border rounded-xl px-3 py-2 mb-1 inline-block max-w-full">
          <p style={{ fontSize: '0.875rem', lineHeight: '1.5', wordBreak: 'break-word' }}>
            {message.content}
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          <p className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
            {message.timestamp}
          </p>
          
          {message.reactions && message.reactions.length > 0 && (
            <div className="flex items-center gap-1">
              {message.reactions.map((reaction, idx) => (
                <span
                  key={idx}
                  className="px-1.5 py-0.5 bg-secondary rounded-full"
                  style={{ fontSize: '0.75rem' }}
                >
                  {reaction.emoji} {reaction.count}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
