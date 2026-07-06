# DevFocus Backend
 
![CI](https://github.com/OLekgetho/DevFocus/actions/workflows/ci.yml/badge.svg)
![Deploy](https://github.com/OLekgetho/DevFocus/actions/workflows/deploy.yml/badge.svg)
 
DevFocus is a developer productivity platform that combines Pomodoro-style focus sessions with GitHub activity tracking. Developers log in with their GitHub account, run focus sessions against real work, and see their productivity connected to actual commits and repositories.
 
This repository contains the backend: a Spring Boot multi-module project structured as microservices and deployed as a monolith.
 
## Tech Stack
 
| Layer | Technology |
|---|---|
| Language / Framework | Java 25, Spring Boot 4 |
| Build | Maven (multi-module) |
| Database | PostgreSQL 16, Flyway migrations |
| Identity | AWS Cognito (admin auth flows) + GitHub OAuth |
| Token security | AES-256-GCM encryption for stored GitHub tokens |
| Infrastructure | AWS EC2 (af-south-1), Nginx, Let's Encrypt SSL |
| Secrets | AWS Systems Manager Parameter Store + IAM instance roles |
| CI/CD | GitHub Actions with OIDC federation (no stored AWS keys) |
 
## Architecture
 
The codebase is organised as independent service modules sharing a common core, deployed together as a single JAR (monolith-first strategy — the service boundaries exist in code, ready to be split when scale demands it).
 
```
devfocus-backend/
├── devfocus-shared/          # Cross-cutting: security, exceptions, base entities, API response envelope
├── devfocus-gateway/         # Entry point and routing
├── devfocus-auth-service/    # GitHub OAuth + Cognito integration (complete)
├── devfocus-user-service/    # User profiles (planned)
├── devfocus-github-service/  # GitHub activity integration (planned)
├── devfocus-pomodoro-service/# Focus session management (planned)
├── devfocus-task-service/    # Task tracking (planned)
└── devfocus-notification-service/ # Notifications (planned)
```
 
### Authentication flow
 
1. Client requests the GitHub authorization URL
2. User approves on GitHub; the callback code is exchanged for a GitHub access token
3. The user's GitHub profile is fetched (with a `/user/emails` fallback for private emails)
4. The GitHub token is encrypted (AES-256-GCM) before storage — plaintext tokens never touch the database
5. A Cognito user is created or resolved (username = immutable GitHub ID), and JWTs are issued via an ephemeral-password admin flow
6. Subsequent requests are authenticated by validating the Cognito JWT against its JWK set
### Security posture
 
- Least-privilege IAM throughout: scoped policies for local dev, EC2 instance role, and CI
- GitHub Actions authenticates to AWS via OIDC federation — no long-lived cloud credentials in CI
- Deploy pipeline opens SSH to the runner's IP only for the duration of the deployment, then closes it
- Secrets live in Parameter Store (SecureString), loaded at boot via instance role
- Separate OAuth apps and encryption keys per environment
- CodeQL, Dependabot, and secret scanning enabled
## Local Development
 
### Prerequisites
 
- Java 25 (Amazon Corretto recommended)
- Docker (for PostgreSQL)
- An AWS account with a Cognito user pool, and a GitHub OAuth App
### Setup
 
1. Clone the repo and copy the environment template:
```bash
   cp .env.example .env          # fill in your values
   cp docker/.env.example docker/.env
```
2. Start PostgreSQL:
```bash
   cd docker && docker compose up -d
```
3. Run the auth service (loads env from `.env`):
```bash
   mvn spring-boot:run -pl devfocus-auth-service
```
4. Verify:
```bash
   curl http://localhost:8081/actuator/health
```
 
## Deployment
 
Merges to `main` deploy automatically to EC2 via GitHub Actions:
 
```
build fat JAR → assume AWS role via OIDC → open SSH for runner IP →
SCP JAR → restart systemd service → poll health endpoint → close SSH
```
 
Markdown-only changes are excluded from triggering deployments. Deployments are tracked in the repository's [Deployments](../../deployments) panel.
 
## Project Status
 
The auth service is complete and live: GitHub OAuth login, token refresh, logout, and GitHub connect/disconnect, all tested end-to-end in production. Remaining services are being built incrementally — see [Issues](../../issues) for the roadmap.
 
## License
 
This project is currently unlicensed (all rights reserved) while under active development.
