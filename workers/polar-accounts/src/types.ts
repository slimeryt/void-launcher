export interface Env {
  DB: D1Database;
  ADMIN_TOKEN?: string;
}

/** Developer Account application status (admin-gated). */
export type DeveloperAccountStatus = "none" | "pending" | "approved" | "denied";

/** Developer Beta program enrollment (self-serve once Developer Account is approved). */
export type EnrollmentStatus = "none" | "pending" | "approved" | "denied";

export interface UserRow {
  id: string;
  email: string;
  password_hash: string;
  display_name: string;
  created_at: string;
  developer_status?: DeveloperAccountStatus;
  developer_requested_at?: string | null;
  developer_reviewed_at?: string | null;
}

export interface EnrollmentRow {
  user_id: string;
  status: EnrollmentStatus;
  requested_at: string | null;
  reviewed_at: string | null;
  note: string;
}
