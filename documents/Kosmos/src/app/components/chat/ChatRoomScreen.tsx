import { useState } from 'react';
import { ArrowLeft, MoreVertical } from 'lucide-react';
import { MessageBubble, MessageData } from './MessageBubble';
import { MessageInput } from './MessageInput';

const mockMessages: MessageData[] = [
  {
    id: '1',
    type: 'system',
    content: 'Alice Chen created this chat',
    timestamp: 'Jan 8'
  },
  {
    id: '2',
    type: 'user',
    sender: { name: 'Alice Chen', avatar: 'A' },
    content: 'Hey team! I started working on the new onboarding flow designs. Would love to get your feedback.',
    timestamp: '10:30 AM',
    isOwn: false
  },
  {
    id: '3',
    type: 'user',
    sender: { name: 'Bob Smith', avatar: 'B' },
    content: 'Sounds great! When can we expect the first draft?',
    timestamp: '10:32 AM',
    isOwn: false
  },
  {
    id: '4',
    type: 'user',
    sender: { name: 'Alice Chen', avatar: 'A' },
    content: 'I should have something ready by end of day tomorrow.',
    timestamp: '10:35 AM',
    isOwn: false
  },
  {
    id: '5',
    type: 'task',
    sender: { name: 'Alice Chen', avatar: 'A' },
    content: 'Created this task to track the design work',
    timestamp: '10:36 AM',
    isOwn: false,
    taskLink: {
      title: 'Design new onboarding flow for mobile app',
      status: 'In Progress'
    }
  },
  {
    id: '6',
    type: 'system',
    content: 'Today',
    timestamp: ''
  },
  {
    id: '7',
    type: 'user',
    sender: { name: 'Carol Davis', avatar: 'C' },
    content: 'I reviewed the initial concepts. Looking good so far!',
    timestamp: '9:15 AM',
    isOwn: false,
    reactions: [{ emoji: '👍', count: 2 }]
  },
  {
    id: '8',
    type: 'user',
    sender: { name: 'You', avatar: 'Y' },
    content: 'I think we should use the purple variant for the primary buttons',
    timestamp: '9:45 AM',
    isOwn: true
  }
];

interface ChatRoomScreenProps {
  chatId: string;
  onBack?: () => void;
}

export function ChatRoomScreen({ chatId, onBack }: ChatRoomScreenProps) {
  const [replyTo, setReplyTo] = useState<{ sender: string; message: string } | null>(null);

  const chatName = 'Design Discussion';
  const projectName = 'Mobile App Redesign';
  const memberCount = 4;

  const handleSendMessage = (message: string) => {
    console.log('Sending message:', message);
    setReplyTo(null);
  };

  // Group messages by sender
  const messagesWithGrouping = mockMessages.map((msg, idx) => {
    const prevMsg = idx > 0 ? mockMessages[idx - 1] : null;
    const showAvatar = !prevMsg || 
                       prevMsg.type !== msg.type || 
                       prevMsg.sender?.name !== msg.sender?.name ||
                       msg.type === 'system';
    const showName = showAvatar && msg.type !== 'system';
    
    return { ...msg, showAvatar, showName };
  });

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border">
        <div className="px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <button
              onClick={onBack}
              className="p-2 hover:bg-secondary rounded-lg transition-colors flex-shrink-0"
            >
              <ArrowLeft size={22} className="text-foreground" />
            </button>
            <div className="flex-1 min-w-0">
              <h2 className="truncate" style={{ fontWeight: '600', fontSize: '1rem' }}>
                {chatName}
              </h2>
              <p className="text-muted-foreground truncate" style={{ fontSize: '0.75rem' }}>
                {projectName} · {memberCount} members
              </p>
            </div>
          </div>
          <button className="p-2 hover:bg-secondary rounded-lg transition-colors flex-shrink-0">
            <MoreVertical size={22} className="text-foreground" />
          </button>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {messagesWithGrouping.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            showAvatar={message.showAvatar}
            showName={message.showName}
          />
        ))}
      </div>

      {/* Message Input */}
      <MessageInput
        onSend={handleSendMessage}
        replyTo={replyTo}
        onCancelReply={() => setReplyTo(null)}
      />
    </div>
  );
}
