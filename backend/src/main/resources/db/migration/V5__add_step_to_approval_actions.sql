ALTER TABLE approval_actions
    ADD COLUMN IF NOT EXISTS step_id uuid;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'approval_actions' AND constraint_name = 'fk_approval_step'
    ) THEN
        ALTER TABLE approval_actions
            ADD CONSTRAINT fk_approval_step
                FOREIGN KEY (step_id) REFERENCES workflow_steps (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_approval_actions_step_id
    ON approval_actions (step_id);
