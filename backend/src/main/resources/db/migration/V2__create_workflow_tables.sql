CREATE TABLE IF NOT EXISTS request_types (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL UNIQUE,
    description text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflows (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL,
    request_type_id uuid NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_workflow_request_type FOREIGN KEY (request_type_id) REFERENCES request_types (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS workflow_steps (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid NOT NULL,
    step_name varchar(150) NOT NULL,
    order_index integer NOT NULL,
    approver_role varchar(100) NOT NULL,
    approver_user_id uuid NULL,
    requires_comment boolean NOT NULL DEFAULT false,
    CONSTRAINT fk_workflow_step_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type_id uuid NOT NULL,
    submitted_by_id uuid NOT NULL,
    workflow_id uuid,
    title varchar(200) NOT NULL,
    description text NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','IN_REVIEW','APPROVED','REJECTED','CANCELLED')),
    current_step_index integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    resolved_at timestamptz,
    last_comment text,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_request_type FOREIGN KEY (request_type_id) REFERENCES request_types (id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_user FOREIGN KEY (submitted_by_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS approval_actions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL,
    actor_id uuid NOT NULL,
    action_type varchar(30) NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_approval_request FOREIGN KEY (request_id) REFERENCES requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_steps_order
    ON workflow_steps (workflow_id, order_index);

CREATE INDEX IF NOT EXISTS ix_requests_submitted_by_status
    ON requests (submitted_by_id, status);

CREATE INDEX IF NOT EXISTS ix_approval_actions_request_id
    ON approval_actions (request_id);

INSERT INTO request_types (name, description, active)
VALUES ('Expense Reimbursement', 'Travel and business expense reimbursement requests', true)
ON CONFLICT (name) DO NOTHING;
