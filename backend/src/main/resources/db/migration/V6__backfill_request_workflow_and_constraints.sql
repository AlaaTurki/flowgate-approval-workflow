ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS workflow_id uuid;

UPDATE requests r
SET workflow_id = (
    SELECT w.id
    FROM workflows w
    WHERE w.request_type_id = r.request_type_id
      AND w.active = true
    ORDER BY w.created_at ASC, w.id
    LIMIT 1
)
WHERE r.workflow_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM workflows w
      WHERE w.request_type_id = r.request_type_id
        AND w.active = true
  );

UPDATE requests r
SET workflow_id = (
    SELECT w.id
    FROM workflows w
    WHERE w.request_type_id = r.request_type_id
    ORDER BY w.created_at ASC, w.id
    LIMIT 1
)
WHERE r.workflow_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'requests' AND constraint_name = 'fk_request_workflow'
    ) THEN
        ALTER TABLE requests
            ADD CONSTRAINT fk_request_workflow
            FOREIGN KEY (workflow_id) REFERENCES workflows (id) ON DELETE RESTRICT;
    END IF;
END $$;

ALTER TABLE requests
    ALTER COLUMN workflow_id SET NOT NULL;

ALTER TABLE requests
    ALTER COLUMN status SET DEFAULT 'DRAFT';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'requests_status_check'
    ) THEN
        ALTER TABLE requests
            ADD CONSTRAINT requests_status_check
            CHECK (status IN ('DRAFT','SUBMITTED','IN_REVIEW','APPROVED','REJECTED','CANCELLED'))
            NOT VALID;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_steps_order
    ON workflow_steps (workflow_id, order_index);

CREATE UNIQUE INDEX IF NOT EXISTS ux_active_workflow_per_request_type
    ON workflows (request_type_id)
    WHERE active = true;

CREATE INDEX IF NOT EXISTS ix_requests_submitted_by_status
    ON requests (submitted_by_id, status);

CREATE INDEX IF NOT EXISTS ix_requests_workflow_id
    ON requests (workflow_id);

CREATE INDEX IF NOT EXISTS ix_requests_request_type_id
    ON requests (request_type_id);

CREATE INDEX IF NOT EXISTS ix_approval_actions_request_id
    ON approval_actions (request_id);
