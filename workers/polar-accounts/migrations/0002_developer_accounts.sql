-- Two-step Developer access:
-- 1) developer_status on users (admin-approved Developer Account)
-- 2) enrollments (self-enroll in Developer Beta once you have a Developer Account)

ALTER TABLE users ADD COLUMN developer_status TEXT NOT NULL DEFAULT 'none';
ALTER TABLE users ADD COLUMN developer_requested_at TEXT;
ALTER TABLE users ADD COLUMN developer_reviewed_at TEXT;

-- Carry over anyone already enrollment-approved as a Developer Account.
UPDATE users
SET developer_status = 'approved',
    developer_reviewed_at = COALESCE(
      (SELECT reviewed_at FROM enrollments WHERE enrollments.user_id = users.id),
      datetime('now')
    )
WHERE id IN (SELECT user_id FROM enrollments WHERE status = 'approved');

-- Pending enrollment requests become Developer Account requests.
UPDATE users
SET developer_status = 'pending',
    developer_requested_at = COALESCE(
      (SELECT requested_at FROM enrollments WHERE enrollments.user_id = users.id),
      datetime('now')
    )
WHERE id IN (SELECT user_id FROM enrollments WHERE status = 'pending')
  AND developer_status = 'none';

UPDATE users
SET developer_status = 'denied',
    developer_reviewed_at = COALESCE(
      (SELECT reviewed_at FROM enrollments WHERE enrollments.user_id = users.id),
      datetime('now')
    )
WHERE id IN (SELECT user_id FROM enrollments WHERE status = 'denied')
  AND developer_status = 'none';

-- Clear pending/denied enrollments — enrollment is now self-serve after Developer Account.
UPDATE enrollments SET status = 'none', requested_at = NULL, reviewed_at = NULL
WHERE status IN ('pending', 'denied');
