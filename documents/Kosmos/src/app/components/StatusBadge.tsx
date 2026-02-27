interface StatusBadgeProps {
  status: 'Active' | 'Archived';
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const isActive = status === 'Active';
  
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full ${
        isActive
          ? 'bg-primary/20 text-primary border border-primary/30'
          : 'bg-muted text-muted-foreground border border-muted'
      }`}
      style={{ fontSize: '0.75rem', fontWeight: '500' }}
    >
      {status}
    </span>
  );
}