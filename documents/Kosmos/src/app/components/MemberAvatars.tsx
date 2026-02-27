interface MemberAvatarsProps {
  count: number;
  maxDisplay?: number;
  size?: 'sm' | 'md';
}

export function MemberAvatars({ count, maxDisplay = 4, size = 'md' }: MemberAvatarsProps) {
  const displayCount = Math.min(count, maxDisplay);
  const remaining = count - displayCount;
  const avatarSize = size === 'sm' ? 'w-6 h-6' : 'w-8 h-8';
  const fontSize = size === 'sm' ? '0.625rem' : '0.75rem';
  
  // Generate colors based on index
  const colors = [
    '#7C3AED', '#A855F7', '#8B5CF6', '#6366F1', '#3B82F6'
  ];

  return (
    <div className="flex items-center -space-x-2">
      {Array.from({ length: displayCount }).map((_, i) => (
        <div
          key={i}
          className={`${avatarSize} rounded-full border-2 flex items-center justify-center`}
          style={{
            backgroundColor: colors[i % colors.length],
            borderColor: '#18181D',
            color: 'white',
            fontSize,
            fontWeight: '500',
            boxShadow: '0 2px 4px rgba(0,0,0,0.3)'
          }}
        >
          {String.fromCharCode(65 + i)}
        </div>
      ))}
      {remaining > 0 && (
        <div
          className={`${avatarSize} rounded-full border-2 bg-muted flex items-center justify-center text-muted-foreground`}
          style={{ 
            fontSize, 
            fontWeight: '500',
            borderColor: '#18181D',
            boxShadow: '0 2px 4px rgba(0,0,0,0.3)'
          }}
        >
          +{remaining}
        </div>
      )}
    </div>
  );
}