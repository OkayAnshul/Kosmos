-- ============================================
-- CREATE MISSING task_activity TABLE
-- Run this in Supabase SQL Editor
-- ============================================

CREATE TABLE public.task_activity (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  task_id uuid NOT NULL,
  project_id uuid NOT NULL,
  actor_id uuid NOT NULL,
  actor_name text NOT NULL,
  actor_role text NULL,
  action_type text NOT NULL,
  timestamp bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()))::bigint * 1000),
  changes text NULL,  -- JSON string of List<FieldChange>
  commit_message text NULL,
  auto_description text NOT NULL,
  metadata text NULL,  -- JSON string of Map<String, String>

  CONSTRAINT task_activity_pkey PRIMARY KEY (id),
  CONSTRAINT fk_task_activity_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_task_activity_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_task_activity_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT task_activity_action_type_check CHECK (
    action_type IN (
      'created', 'updated', 'status_changed', 'priority_changed',
      'assigned', 'unassigned', 'description_changed', 'due_date_changed',
      'tags_updated', 'comment_added', 'time_logged', 'dependency_added',
      'dependency_removed', 'subtask_added', 'archived', 'restored', 'deleted'
    )
  )
) TABLESPACE pg_default;

-- Indexes for performance
CREATE INDEX idx_task_activity_task ON task_activity(task_id, timestamp DESC) TABLESPACE pg_default;
CREATE INDEX idx_task_activity_project ON task_activity(project_id) TABLESPACE pg_default;
CREATE INDEX idx_task_activity_actor ON task_activity(actor_id) TABLESPACE pg_default;
CREATE INDEX idx_task_activity_action_type ON task_activity(action_type) TABLESPACE pg_default;

-- Verify table was created
SELECT
  column_name,
  data_type,
  is_nullable
FROM information_schema.columns
WHERE table_name = 'task_activity'
ORDER BY ordinal_position;

-- Expected: 12 columns
SELECT COUNT(*) as task_activity_column_count
FROM information_schema.columns
WHERE table_name = 'task_activity';
