# Books Quest Application

Interactive Books Quest app built with Angular (frontend) and Spring Boot microservices (backend).

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- npm 9+

### Backend

Build all services:
```bash
cd backend
mvn clean install
```

Run each service in a separate terminal:

```bash
cd backend/api/auth
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd backend/api/book
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd backend/api/game
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd backend/internal/manage
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/
```

### Frontend

```bash
cd frontend
npm install
npm start

#To generate a production-ready dist/ folder
#npm run build
#npm run build --configuration production
```

Runs on `http://localhost:4200`

## Services

| Service | Port | Purpose |
|---------|------|---------|
| **auth** | 9092 | Authentication |
| **book** | 9090 | Book catalog and search |
| **game** | 9091 | Game sessions and moves |
| **manage** | ---  | Internal book management |

## Project Structure

```
books-quest/
├── backend/
│   ├── shared/               Shared config & DTOs
│   ├── api/
│   │   ├── auth/             Auth service (port 9092)
│   │   ├── book/             Book service (port 9090)
│   │   └── game/             Game service (port 9091)
│   └── internal/
│       └── manage/           Manage service (no port)
├── frontend/                 Angular app (port 4200)
├── db/                       SQLite databases
├── books/                    JSON books quests
└── README.md
```

## Features

- Browse and search books quests
- Play interactive stories with meaningful choices
- Manage game health and progress
- Save/load game state
- Win by reaching END section or lose if health drops to 0

## Book Format

Books are JSON files with:
- **Metadata**: title, author, difficulty, category, tags
- **Sections**: Story text with choices
- **Game Logic**: Health impacts, consequences

See `BOOK_JSON_TEMPLATE.json` for details.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port already in use | Kill process: `kill <PID>` |
| Java not found | Install Java 17+ |
| Database error | Verify `db/` directory exists |
| Frontend can't connect | Check services running on correct ports |

For backend details, see `backend/README.md`
