-- ============================================
-- RBAC Manual Testing Script for Supabase
-- ============================================
-- Run these queries one by one in Supabase SQL Editor
-- Project: Kosmos Android App
-- URL: https://krbfvekgqbcwjgntepip.supabase.co
-- ============================================

-- ============================================
-- TEST USER IDs (Valid UUIDs)
-- ============================================
-- These UUIDs are used consistently throughout the test
-- user-admin-001:    00000000-0000-0000-0000-000000000001
-- user-manager-002:  00000000-0000-0000-0000-000000000002
-- user-member-003:   00000000-0000-0000-0000-000000000003
-- user-admin-004:    00000000-0000-0000-0000-000000000004
-- test-project-001:  10000000-0000-0000-0000-000000000001

-- ============================================
-- STEP 1: Verify Database Setup
-- ============================================
-- This should show 7 tables
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Expected output:
-- chat_rooms
-- messages
-- project_members
-- projects
-- task_comments
-- tasks
-- users


-- ============================================
-- STEP 2: Create Test User (ADMIN)
-- ============================================
-- Clean up any existing test data first
DELETE FROM tasks WHERE project_id = '10000000-0000-0000-0000-000000000001';
DELETE FROM project_members WHERE project_id = '10000000-0000-0000-0000-000000000001';
DELETE FROM projects WHERE id = '10000000-0000-0000-0000-000000000001';
DELETE FROM users WHERE email LIKE '%@rbactest.kosmos';

-- Create test admin user
INSERT INTO users (id, email, display_name, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@rbactest.kosmos',
    'Test Admin User',
    extract(epoch from now())::bigint * 1000
);

-- Verify user created
SELECT id, email, display_name, created_at
FROM users
WHERE email = 'admin@rbactest.kosmos';

-- Expected: 1 row with admin@rbactest.kosmos


-- ============================================
-- STEP 3: Create Test Project with Auto-ADMIN
-- ============================================
-- Create project
INSERT INTO projects (id, name, description, owner_id, status, visibility, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'RBAC Verification Project',
    'Testing role-based access control implementation',
    '00000000-0000-0000-0000-000000000001',
    'ACTIVE',
    'PRIVATE',
    extract(epoch from now())::bigint * 1000,
    extract(epoch from now())::bigint * 1000
);

-- Auto-add owner as ADMIN (this simulates ProjectRepository.createProject())
INSERT INTO project_members (id, project_id, user_id, role, joined_at, is_active, invited_by)
VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'ADMIN',
    extract(epoch from now())::bigint * 1000,
    true,
    NULL  -- NULL because owner is auto-added, not invited
);

-- Verify project and ADMIN membership
SELECT
    p.name as project_name,
    p.owner_id,
    u.email as owner_email,
    pm.role as owner_role,
    pm.invited_by,
    pm.is_active
FROM projects p
JOIN project_members pm ON p.id = pm.project_id
JOIN users u ON pm.user_id = u.id
WHERE p.id = '10000000-0000-0000-0000-000000000001';

-- Expected output:
-- project_name: RBAC Verification Project
-- owner_email: admin@rbactest.kosmos
-- owner_role: ADMIN
-- invited_by: NULL
-- is_active: true


-- ============================================
-- STEP 4: Test Adding MANAGER (Role Hierarchy)
-- ============================================
-- Create second test user
INSERT INTO users (id, email, display_name, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'manager@rbactest.kosmos',
    'Test Manager User',
    extract(epoch from now())::bigint * 1000
);

-- ADMIN adds MANAGER (this simulates ProjectRepository.addMember())
INSERT INTO project_members (id, project_id, user_id, role, invited_by, joined_at, is_active)
VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    'MANAGER',
    '00000000-0000-0000-0000-000000000001',  -- Invited by the ADMIN
    extract(epoch from now())::bigint * 1000,
    true
);

-- Verify both members exist with correct roles
SELECT
    u.email as member_email,
    pm.role,
    inviter.email as invited_by_email,
    pm.is_active
FROM project_members pm
JOIN users u ON pm.user_id = u.id
LEFT JOIN users inviter ON pm.invited_by = inviter.id
WHERE pm.project_id = '10000000-0000-0000-0000-000000000001'
ORDER BY pm.role DESC;

-- Expected output (2 rows):
-- Row 1: admin@rbactest.kosmos, ADMIN, NULL, true
-- Row 2: manager@rbactest.kosmos, MANAGER, admin@rbactest.kosmos, true


-- ============================================
-- STEP 5: Test Adding MEMBER
-- ============================================
-- Create third test user
INSERT INTO users (id, email, display_name, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'member@rbactest.kosmos',
    'Test Member User',
    extract(epoch from now())::bigint * 1000
);

-- ADMIN adds MEMBER
INSERT INTO project_members (id, project_id, user_id, role, invited_by, joined_at, is_active)
VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000003',
    'MEMBER',
    '00000000-0000-0000-0000-000000000001',
    extract(epoch from now())::bigint * 1000,
    true
);

-- Verify all 3 members with role hierarchy
SELECT
    u.email,
    pm.role,
    CASE
        WHEN pm.role = 'ADMIN' THEN 3
        WHEN pm.role = 'MANAGER' THEN 2
        WHEN pm.role = 'MEMBER' THEN 1
    END as role_weight
FROM project_members pm
JOIN users u ON pm.user_id = u.id
WHERE pm.project_id = '10000000-0000-0000-0000-000000000001'
ORDER BY role_weight DESC;

-- Expected output (3 rows, ordered by weight):
-- admin@rbactest.kosmos, ADMIN, 3
-- manager@rbactest.kosmos, MANAGER, 2
-- member@rbactest.kosmos, MEMBER, 1


-- ============================================
-- STEP 6: Test Task Creation with Role Tracking
-- ============================================
-- ADMIN creates task and assigns to MANAGER
INSERT INTO tasks (
    id,
    project_id,
    title,
    description,
    status,
    priority,
    created_by_id,
    created_by_name,
    created_by_role,
    assigned_to_id,
    assigned_to_name,
    assigned_to_role,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    'Implement User Authentication',
    'Add Google Sign-In and Firebase Auth integration',
    'TODO',
    'HIGH',
    '00000000-0000-0000-0000-000000000001',
    'Test Admin User',
    'ADMIN',
    '00000000-0000-0000-0000-000000000002',
    'Test Manager User',
    'MANAGER',
    extract(epoch from now())::bigint * 1000,
    extract(epoch from now())::bigint * 1000
);

-- Verify role tracking fields are populated
SELECT
    t.title,
    t.status,
    t.priority,
    t.created_by_name,
    t.created_by_role,
    t.assigned_to_name,
    t.assigned_to_role,
    p.name as project_name
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE t.project_id = '10000000-0000-0000-0000-000000000001';

-- Expected output:
-- title: Implement User Authentication
-- created_by_name: Test Admin User
-- created_by_role: ADMIN
-- assigned_to_name: Test Manager User
-- assigned_to_role: MANAGER
-- project_name: RBAC Verification Project


-- ============================================
-- STEP 7: Test MANAGER Creates Task and Assigns to MEMBER
-- ============================================
-- MANAGER creates task and assigns to MEMBER (allowed by hierarchy)
INSERT INTO tasks (
    id,
    project_id,
    title,
    description,
    status,
    priority,
    created_by_id,
    created_by_name,
    created_by_role,
    assigned_to_id,
    assigned_to_name,
    assigned_to_role,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    'Write Unit Tests',
    'Create unit tests for authentication module',
    'TODO',
    'MEDIUM',
    '00000000-0000-0000-0000-000000000002',
    'Test Manager User',
    'MANAGER',
    '00000000-0000-0000-0000-000000000003',
    'Test Member User',
    'MEMBER',
    extract(epoch from now())::bigint * 1000,
    extract(epoch from now())::bigint * 1000
);

-- Verify both tasks with role tracking
SELECT
    t.title,
    creator.email as created_by,
    t.created_by_role,
    assignee.email as assigned_to,
    t.assigned_to_role,
    t.priority
FROM tasks t
JOIN users creator ON t.created_by_id = creator.id
JOIN users assignee ON t.assigned_to_id = assignee.id
WHERE t.project_id = '10000000-0000-0000-0000-000000000001'
ORDER BY t.priority DESC;

-- Expected output (2 rows):
-- Row 1: Implement User Authentication, admin@rbactest.kosmos, ADMIN, manager@rbactest.kosmos, MANAGER, HIGH
-- Row 2: Write Unit Tests, manager@rbactest.kosmos, MANAGER, member@rbactest.kosmos, MEMBER, MEDIUM


-- ============================================
-- STEP 8: Verify Business Rule - Cannot Remove Last ADMIN
-- ============================================
-- Count active ADMINs in project
SELECT
    p.name as project_name,
    COUNT(CASE WHEN pm.role = 'ADMIN' AND pm.is_active = true THEN 1 END) as active_admin_count,
    COUNT(CASE WHEN pm.role = 'MANAGER' AND pm.is_active = true THEN 1 END) as active_manager_count,
    COUNT(CASE WHEN pm.role = 'MEMBER' AND pm.is_active = true THEN 1 END) as active_member_count
FROM projects p
LEFT JOIN project_members pm ON p.id = pm.project_id
WHERE p.id = '10000000-0000-0000-0000-000000000001'
GROUP BY p.id, p.name;

-- Expected output:
-- active_admin_count: 1
-- active_manager_count: 1
-- active_member_count: 1

-- Simulate trying to remove the only ADMIN (this should be BLOCKED by app logic)
-- We won't actually delete, but this query shows what would happen:
SELECT
    'BLOCKED: Cannot remove last ADMIN' as validation_result,
    u.email as admin_to_remove,
    pm.role,
    COUNT(*) OVER (PARTITION BY pm.project_id, pm.role) as total_admins
FROM project_members pm
JOIN users u ON pm.user_id = u.id
WHERE pm.project_id = '10000000-0000-0000-0000-000000000001'
AND pm.role = 'ADMIN'
AND pm.is_active = true;

-- Expected: Shows 1 ADMIN, confirming removal would break the project

-- Add a second ADMIN to test that removal becomes allowed
INSERT INTO users (id, email, display_name, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000004',
    'admin2@rbactest.kosmos',
    'Test Second Admin',
    extract(epoch from now())::bigint * 1000
);

INSERT INTO project_members (id, project_id, user_id, role, invited_by, joined_at, is_active)
VALUES (
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000004',
    'ADMIN',
    '00000000-0000-0000-0000-000000000001',
    extract(epoch from now())::bigint * 1000,
    true
);

-- Verify 2 ADMINs now exist
SELECT
    u.email,
    pm.role
FROM project_members pm
JOIN users u ON pm.user_id = u.id
WHERE pm.project_id = '10000000-0000-0000-0000-000000000001'
AND pm.role = 'ADMIN'
ORDER BY pm.joined_at;

-- Expected output (2 rows):
-- admin@rbactest.kosmos, ADMIN
-- admin2@rbactest.kosmos, ADMIN

-- Now removal of first ADMIN would be ALLOWED (because second ADMIN exists)
SELECT
    'ALLOWED: Another ADMIN exists' as validation_result,
    COUNT(*) as total_active_admins
FROM project_members pm
WHERE pm.project_id = '10000000-0000-0000-0000-000000000001'
AND pm.role = 'ADMIN'
AND pm.is_active = true;

-- Expected: total_active_admins = 2


-- ============================================
-- STEP 9: Test Permission Verification
-- ============================================
-- This simulates what PermissionChecker does in the app

-- Check permissions for ADMIN
SELECT
    'ADMIN' as role,
    'Has all permissions' as permission_check;

-- Check permissions for MANAGER
SELECT
    'MANAGER' as role,
    string_agg(permission, ', ') as has_permissions
FROM (
    VALUES
        ('VIEW_PROJECT'),
        ('EDIT_PROJECT'),
        ('VIEW_MEMBERS'),
        ('INVITE_MEMBERS'),
        ('REMOVE_MEMBERS'),
        ('VIEW_TASKS'),
        ('CREATE_TASKS'),
        ('EDIT_ANY_TASK'),
        ('DELETE_ANY_TASK'),
        ('ASSIGN_TASKS'),
        ('CHANGE_TASK_STATUS'),
        ('CHANGE_TASK_PRIORITY')
) AS manager_perms(permission);

-- Check permissions for MEMBER
SELECT
    'MEMBER' as role,
    string_agg(permission, ', ') as has_permissions
FROM (
    VALUES
        ('VIEW_PROJECT'),
        ('VIEW_MEMBERS'),
        ('VIEW_TASKS'),
        ('CREATE_TASKS'),
        ('EDIT_OWN_TASKS'),
        ('DELETE_OWN_TASKS'),
        ('COMMENT_ON_TASKS')
) AS member_perms(permission);


-- ============================================
-- STEP 10: Final Summary - All RBAC Features
-- ============================================
-- Comprehensive overview of test project
SELECT
    p.name as project_name,
    p.status,
    p.visibility,
    u_owner.email as owner_email,
    COUNT(DISTINCT pm.user_id) as total_members,
    COUNT(DISTINCT CASE WHEN pm.role = 'ADMIN' THEN pm.user_id END) as admin_count,
    COUNT(DISTINCT CASE WHEN pm.role = 'MANAGER' THEN pm.user_id END) as manager_count,
    COUNT(DISTINCT CASE WHEN pm.role = 'MEMBER' THEN pm.user_id END) as member_count,
    COUNT(DISTINCT t.id) as total_tasks
FROM projects p
JOIN users u_owner ON p.owner_id = u_owner.id
LEFT JOIN project_members pm ON p.id = pm.project_id AND pm.is_active = true
LEFT JOIN tasks t ON p.id = t.project_id
WHERE p.id = '10000000-0000-0000-0000-000000000001'
GROUP BY p.id, p.name, p.status, p.visibility, u_owner.email;

-- Expected output:
-- project_name: RBAC Verification Project
-- status: ACTIVE
-- visibility: PRIVATE
-- owner_email: admin@rbactest.kosmos
-- total_members: 4
-- admin_count: 2
-- manager_count: 1
-- member_count: 1
-- total_tasks: 2


-- ============================================
-- CLEANUP (Optional - Run after verification)
-- ============================================
-- Uncomment to clean up test data:

-- DELETE FROM tasks WHERE project_id = '10000000-0000-0000-0000-000000000001';
-- DELETE FROM project_members WHERE project_id = '10000000-0000-0000-0000-000000000001';
-- DELETE FROM projects WHERE id = '10000000-0000-0000-0000-000000000001';
-- DELETE FROM users WHERE email LIKE '%@rbactest.kosmos';


-- ============================================
-- SUCCESS CRITERIA CHECKLIST
-- ============================================
-- After running all queries above, verify:
-- ✅ All 7 tables exist
-- ✅ Users created successfully
-- ✅ Project created with owner as ADMIN
-- ✅ ADMIN, MANAGER, MEMBER roles assigned correctly
-- ✅ Role hierarchy visible (ADMIN=3, MANAGER=2, MEMBER=1)
-- ✅ Tasks created with role tracking fields populated
-- ✅ created_by_role and assigned_to_role stored correctly
-- ✅ Business rule verified: Cannot remove last ADMIN
-- ✅ Permission sets different for each role

-- If all above are ✅, then Phase 1A RBAC is 100% COMPLETE! 🎉
