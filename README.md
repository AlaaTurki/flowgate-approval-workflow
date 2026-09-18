# FlowGate

FlowGate is a full-stack approval workflow platform that lets organizations define multi-step approval processes and route requests such as leave, purchases, and expenses through them.

Admins configure request types and approval steps, employees submit requests, and managers approve or reject them with comments. Every action is recorded in an audit trail, and the workflow engine enforces who can act at each step.

## Key Features

- Configurable multi-step approval workflows per request type
- Role-based access control with Employee, Manager, and Admin roles
- JWT authentication
- Request lifecycle tracking: Draft -> Submitted -> In Review -> Approved / Rejected / Cancelled
- Complete audit trail and comment history
- Approver inbox, request timeline, and status dashboard
- Email notifications for each workflow event

## Tech Stack

- **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway
- **Frontend:** Angular 20, TypeScript, SCSS, Reactive Forms
- **Database:** PostgreSQL
- **DevOps:** Docker Compose, GitHub Actions
- **Testing:** JUnit 5, Testcontainers, Jasmine/Jest

## Repository Structure

```text
.
├── backend/     # Spring Boot API and workflow engine
└── frontend/    # Angular web application
```

## Getting Started

### Prerequisites

- Java 21
- Node.js and npm
- Angular CLI 20
- PostgreSQL, or Docker with Docker Compose

### Setup

The project is currently being initialized. Once the backend and frontend applications are added:

1. Configure the PostgreSQL connection and JWT settings for the backend.
2. Run the database migrations with Flyway.
3. Start the Spring Boot backend from `backend/`.
4. Install frontend dependencies and start the Angular application from `frontend/`.

Detailed commands and environment variable documentation will be added as the application configuration is introduced.

## Workflow Lifecycle

Requests move through the following lifecycle:

```text
Draft -> Submitted -> In Review -> Approved
                              └──> Rejected
                              └──> Cancelled
```

Each transition is authorized according to the requester's role and the configured approval workflow. Workflow actions, comments, and status changes are retained in the audit trail.

## Testing

The planned test stack includes:

- JUnit 5 for backend unit and integration tests
- Testcontainers for PostgreSQL-backed integration tests
- Jasmine/Jest for frontend tests

Run the project-specific test commands from `backend/` and `frontend/` once their build files are available.

## DevOps

Deployment and local infrastructure will use Docker Compose and GitHub Actions. Container, environment, and CI/CD instructions will be documented here as those configurations are added.

## License

License information has not yet been defined.