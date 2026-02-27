export function ProjectCardSkeleton() {
  return (
    <div className="bg-card border border-border rounded-xl p-4 animate-pulse">
      {/* Header */}
      <div className="flex items-start justify-between mb-2">
        <div className="flex-1">
          <div className="h-5 bg-secondary rounded w-2/3 mb-2"></div>
          <div className="h-4 bg-secondary rounded w-full mb-1"></div>
          <div className="h-4 bg-secondary rounded w-4/5"></div>
        </div>
        <div className="ml-2 w-5 h-5 bg-secondary rounded"></div>
      </div>

      {/* Status Badge */}
      <div className="mb-3">
        <div className="h-5 bg-secondary rounded-full w-16"></div>
      </div>

      {/* Stats Row */}
      <div className="flex items-center gap-4 mb-3">
        <div className="h-4 bg-secondary rounded w-8"></div>
        <div className="h-4 bg-secondary rounded w-8"></div>
        <div className="h-4 bg-secondary rounded w-8"></div>
      </div>

      {/* Progress Bar */}
      <div className="mb-3">
        <div className="flex justify-between mb-1.5">
          <div className="h-3 bg-secondary rounded w-24"></div>
          <div className="h-3 bg-secondary rounded w-8"></div>
        </div>
        <div className="w-full h-1.5 bg-secondary rounded-full"></div>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <div className="flex -space-x-2">
          <div className="w-6 h-6 bg-secondary rounded-full border-2 border-white"></div>
          <div className="w-6 h-6 bg-secondary rounded-full border-2 border-white"></div>
          <div className="w-6 h-6 bg-secondary rounded-full border-2 border-white"></div>
        </div>
        <div className="h-3 bg-secondary rounded w-16"></div>
      </div>
    </div>
  );
}

export function ProjectListSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      {/* Top App Bar */}
      <div className="bg-card border-b border-border">
        <div className="px-4 py-3 flex items-center justify-between">
          <div className="h-6 bg-secondary rounded w-24"></div>
          <div className="w-5 h-5 bg-secondary rounded"></div>
        </div>
      </div>

      {/* Content */}
      <div className="px-4 py-4">
        {/* Search Bar */}
        <div className="mb-4">
          <div className="h-12 bg-secondary rounded-xl"></div>
        </div>

        {/* Filter Chips */}
        <div className="flex gap-2 mb-4">
          <div className="h-9 bg-secondary rounded-lg w-16"></div>
          <div className="h-9 bg-secondary rounded-lg w-16"></div>
          <div className="h-9 bg-secondary rounded-lg w-20"></div>
        </div>

        {/* Project Cards */}
        <div className="space-y-3">
          <ProjectCardSkeleton />
          <ProjectCardSkeleton />
          <ProjectCardSkeleton />
        </div>
      </div>
    </div>
  );
}
