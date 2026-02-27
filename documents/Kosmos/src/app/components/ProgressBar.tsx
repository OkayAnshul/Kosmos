interface ProgressBarProps {
  value: number;
  max: number;
}

export function ProgressBar({ value, max }: ProgressBarProps) {
  const percentage = max > 0 ? Math.round((value / max) * 100) : 0;
  
  return (
    <div className="w-full">
      <div className="flex items-center justify-between mb-1.5">
        <span className="text-muted-foreground" style={{ fontSize: '0.75rem' }}>
          {value} of {max} tasks completed
        </span>
        <span className="text-muted-foreground" style={{ fontSize: '0.75rem', fontWeight: '500' }}>
          {percentage}%
        </span>
      </div>
      <div className="w-full h-1.5 bg-secondary rounded-full overflow-hidden">
        <div
          className="h-full bg-primary rounded-full transition-all duration-300"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
