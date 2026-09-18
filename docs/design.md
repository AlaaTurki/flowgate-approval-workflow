FlowGate — Phase 1 Design

Overview

FlowGate is an approval-workflow platform where employees submit requests that move through an ordered set of approval steps. Admins define workflows and managers approve or reject requests. The design below captures user stories, roles, domain model, lifecycle, ER diagram reference (PlantUML), and API endpoints.

User stories

- As an Employee, I can create a request (draft), submit it, view its timeline, and cancel it.
- As a Manager, I can view my pending approvals, open a request, approve or reject it with a comment.
- As an Admin, I can define request types, build workflows with ordered steps, assign approver roles or specific users to each step, and manage users & roles.
- As any user, I can see a history (audit) of actions taken on a request.

Roles

- Employee: create requests, view own requests.
- Manager: act on requests assigned to them (or their role) when it is their configured step.
- Admin: manage users, roles, request types, and workflows; view audit and system settings.

Domain entities (summary)

- User (id, username, email, password_hash, full_name, enabled, created_at)
- Role (id, name, description)
- RequestType (id, code, name, description, payload_schema)
- Workflow (id, request_type_id, name, description, active)
- WorkflowStep (id, workflow_id, step_order, approver_role_id, approver_user_id, require_comment_on_reject)
- Request (id, request_type_id, creator_id, payload (JSON), status, created_at, submitted_at, closed_at)
- ApprovalAction (id, request_id, step_id, actor_id, action (APPROVE/REJECT/CANCEL), comment, created_at)
- Comment (id, request_id, author_id, content, created_at)
- Notification (id, user_id, type, payload, read, created_at)

Key relationships

- User has many Roles (many-to-many)
- RequestType 1—* Workflow
- Workflow 1—* WorkflowStep (ordered by step_order)
- Request references a Workflow (snapshot of workflow_id when submitted) and has many ApprovalActions and Comments
- ApprovalAction references the WorkflowStep executed and the acting User

Request lifecycle

Draft -> Submitted -> In Review -> Approved / Rejected / Cancelled

- Draft: creator may edit or delete.
- Submitted: workflow attached, first step owner receives notification, request is In Review.
- In Review: current approver(s) may APPROVE or REJECT. On APPROVE move to next step or finish as Approved. On REJECT move to Rejected (require comment if configured).
- Approved/Rejected/Cancelled: terminal states; no further actions allowed.

ER diagram

A PlantUML ER model is available at docs/diagrams/flowgate-er.puml — render with PlantUML to view a clean diagram.

API endpoints (overview)

Authentication
- POST /api/auth/register: Register user (Admin-only or open with invite flow)
- POST /api/auth/login: {username,password} -> {accessToken}

Users & Roles
- GET /api/users (Admin) — list, pagination
- GET /api/users/{id}
- POST /api/users (Admin) — create
- PUT /api/users/{id}
- GET /api/roles
- POST /api/roles (Admin)

Request types & workflows
- GET /api/request-types
- POST /api/request-types (Admin)
- GET /api/workflows
- POST /api/workflows (Admin)
- GET /api/workflows/{id}/steps
- PUT /api/workflows/{id}/steps (Admin)

Requests & approvals
- GET /api/requests — list with filters: status, type, createdBy, dateFrom/dateTo, page, size, sort
- POST /api/requests — create draft
- GET /api/requests/{id}
- PUT /api/requests/{id} — edit draft
- POST /api/requests/{id}/submit — attach workflow and set status=SUBMITTED
- POST /api/requests/{id}/cancel — cancel request (creator only)
- POST /api/requests/{id}/actions/approve — {comment?}
- POST /api/requests/{id}/actions/reject — {comment required if configured}
- GET /api/requests/{id}/timeline — combined ApprovalAction + Comments

Comments & notifications
- POST /api/requests/{id}/comments
- GET /api/users/{id}/notifications
- POST /api/notifications/mark-read

Admin / audit
- GET /api/audit/requests/{id}/actions
- GET /api/reports/dashboard — counts by status, average approval time

Pagination & filtering
- Use standard query params: page (0-based), size, sort=field,asc|desc
- Filtering via query params: status=IN_REVIEW&type=PURCHASE&from=2026-01-01&to=2026-09-01

Security & CORS

- Role-based method security (e.g., @PreAuthorize("hasRole('ADMIN')")) and endpoint checks so only allowed users act.
- Configure CORS to allow frontend dev server origin (http://localhost:4200).

Next steps

- Render PlantUML to an image for inclusion in the README (optional).
- Phase 2: implement backend foundation (package layout, Flyway V1, JPA entities, DTOs/mappers, global exception handler, OpenAPI).