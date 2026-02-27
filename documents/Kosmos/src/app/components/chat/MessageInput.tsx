import { useState } from 'react';
import { Send, X } from 'lucide-react';

interface MessageInputProps {
  onSend?: (message: string) => void;
  replyTo?: {
    sender: string;
    message: string;
  } | null;
  onCancelReply?: () => void;
}

export function MessageInput({ onSend, replyTo, onCancelReply }: MessageInputProps) {
  const [message, setMessage] = useState('');

  const handleSend = () => {
    if (message.trim()) {
      onSend?.(message);
      setMessage('');
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="bg-card border-t border-border">
      {replyTo && (
        <div className="px-4 pt-3 pb-2 border-b border-border">
          <div className="flex items-start justify-between gap-3 px-3 py-2 bg-secondary rounded-lg">
            <div className="flex-1 min-w-0">
              <p className="text-primary" style={{ fontSize: '0.75rem', fontWeight: '600' }}>
                Replying to {replyTo.sender}
              </p>
              <p className="text-muted-foreground truncate" style={{ fontSize: '0.875rem' }}>
                {replyTo.message}
              </p>
            </div>
            <button
              onClick={onCancelReply}
              className="p-1 hover:bg-muted rounded transition-colors flex-shrink-0"
            >
              <X size={16} className="text-muted-foreground" />
            </button>
          </div>
        </div>
      )}
      
      <div className="p-4 flex items-end gap-3">
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Type a message..."
          rows={1}
          className="flex-1 px-4 py-3 bg-secondary border-0 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none max-h-32"
          style={{ fontSize: '0.9375rem' }}
        />
        
        {message.trim() && (
          <button
            onClick={handleSend}
            className="p-3 bg-primary text-primary-foreground rounded-xl hover:opacity-90 transition-opacity flex-shrink-0"
          >
            <Send size={20} />
          </button>
        )}
      </div>
    </div>
  );
}
