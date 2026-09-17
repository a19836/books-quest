# Books Quest Application - Implementation

## ✅ Complete & Production-Ready

A fully functional interactive books quest app with:
- Angular 17 responsive frontend
- Spring Boot microservices backend
- SQLite database persistence
- Complete game mechanics and REST API

## 📁 Project Structure

```
books-quest/
├── README.md                          # Main documentation
├── IMPLEMENTATION.md                  # This file
├── BOOK_JSON_TEMPLATE.json            # Template for creating books
│
├── backend/                           # Spring Boot microservices
│   ├── shared/                        # Shared config, DTOs, exceptions
│   ├── api/
│   │   ├── auth/                      # Auth service (9092)
│   │   ├── book/                      # Book service (9090)
│   │   └── game/                      # Game service (9091)
│   ├── internal/
│   │   └── manage/                    # Management service (no port)
│   └── pom.xml                        # Parent POM
│
├── frontend/                          # Angular 17 app
│   ├── src/
│   │   ├── app/
│   │   │   ├── auth/                  # Authentication
│   │   │   │   ├── components/login
│   │   │   │   ├── guards/
│   │   │   │   ├── interceptors/
│   │   │   │   ├── models/
│   │   │   │   └── services/
│   │   │   ├── book/                  # Book catalog
│   │   │   │   ├── components/
│   │   │   │   ├── models/
│   │   │   │   └── services/
│   │   │   ├── game/                  # Game play
│   │   │   │   ├── components/game-player/
│   │   │   │   ├── models/
│   │   │   │   └── services/
│   │   │   ├── shared/                # Shared UI (header, home)
│   │   │   ├── app.routes.ts
│   │   │   └── app.config.ts
│   │   ├── environments/
│   │   ├── main.ts
│   │   └── index.html
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   └── proxy.conf.json                # API proxy config
│
├── books/                             # JSON books
│   ├── dragon-quest.json
│   ├── crystal-caverns.json
│   ├── pirates-jade-sea.json
│   └── the-prisoner.json
│
└── db/                                # SQLite databases
    └── books-quest-dev.db
```

## ✨ Features Implemented

| Feature | Status |
|---------|--------|
| Book listing & search | ✅ |
| Difficulty & category filtering | ✅ |
| Start game & navigation | ✅ |
| Health system (10 HP) | ✅ |
| Consequences (LOSE/GAIN health) | ✅ |
| Save & load progress | ✅ |
| Move history & previous navigation | ✅ |
| Book validation | ✅ |
| REST API | ✅ |
| SQLite persistence | ✅ |
| Responsive UI | ✅ |

## 🏗️ Architecture

### Backend (Microservices)
- **Auth Service** (9092): Authentication & authorization
- **Book Service** (9090): Book catalog and search
- **Game Service** (9091): Game sessions and moves
- **Manage Service** (no port): Internal book management
- **Shared Module**: Common config, DTOs, exceptions

Layered pattern: Controllers → Services → Repositories → Entities

### Frontend (Angular)
- Standalone components with Angular 17
- Services for API calls
- TypeScript interfaces for type safety
- Responsive Bootstrap styling

### Database
- SQLite for persistence
- Auto-created tables via Hibernate
- Books auto-loaded from `books/` directory

## 📡 REST API

### Book Service (`/api/books`)
```
GET /              - List all books
GET /{id}          - Get book by ID
GET /search        - Search books
GET /filter        - Filter books
```

### Game Service (`/api/games`)
```
POST /start/{bookId}           - Start new game
GET /{gameSessionId}           - Get game session
POST /{gameSessionId}/move     - Make a move
POST /{gameSessionId}/save     - Save progress
```

### Auth Service (`/api/auth`)
Authentication endpoints

## 📚 Book Format

Books are JSON files with required structure:

```json
{
  "title": "Book Title",
  "author": "Author Name",
  "difficulty": "EASY|MEDIUM|HARD",
  "sections": [
    {
      "id": 1,
      "text": "Section text",
      "type": "BEGIN|NODE|END",
      "options": [
        {
          "description": "Choice text",
          "gotoId": 100,
          "consequence": {
            "type": "LOSE_HEALTH|GAIN_HEALTH",
            "value": "5",
            "text": "Effect text"
          }
        }
      ]
    }
  ]
}
```

Validation rules:
- Exactly 1 BEGIN section
- At least 1 END section
- All non-END sections must have options
- All references must exist

## Database Schema

### Users Table
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT | Primary key |
| email | VARCHAR | Unique email |
| username | VARCHAR | Username |
| passwordHash | VARCHAR | Hashed password |
| createdAt | TIMESTAMP | User created |
| updatedAt | TIMESTAMP | Last updated |

### Books Table
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT | Primary key |
| title | VARCHAR | Book title |
| author | VARCHAR | Author name |
| difficulty | VARCHAR | EASY, MEDIUM, or HARD |
| category | VARCHAR | Book category |
| description | TEXT | Book description |
| tags | TEXT | Comma-separated tags |
| fastReadingMinutes | INTEGER | Fast read duration |
| slowReadingMinutes | INTEGER | Slow read duration |
| jsonContent | TEXT | Full book JSON structure |

### GameSessions Table
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT | Primary key |
| userId | BIGINT | User identifier |
| bookId | BIGINT | Book reference |
| savedSectionId | INTEGER | Saved section (only updated on save) |
| savedHealth | INTEGER | Saved health (only updated on save) |
| activeSectionId | INTEGER | Current section during gameplay |
| activeHealth | INTEGER | Current health during gameplay |
| status | VARCHAR | ACTIVE, SAVED, or COMPLETED |
| moveHistory | TEXT | JSON array of section IDs visited |
| createdAt | TIMESTAMP | Session created |
| updatedAt | TIMESTAMP | Last updated |

## 🎮 Game Mechanics

- **Starting Health**: 10 HP
- **Choices**: Each option can have consequences
- **Consequences**: LOSE_HEALTH or GAIN_HEALTH
- **Win Condition**: Reach an END section
- **Lose Condition**: Health reaches 0
- **Saves**: Auto-cleared when game completes

## 🚀 Running

```bash
# Backend
cd backend
mvn clean install
cd api/auth && mvn spring-boot:run -Ddb.path=../../../db/ &
cd ../book && mvn spring-boot:run -Ddb.path=../../../db/ &
cd ../game && mvn spring-boot:run -Ddb.path=../../../db/ &
cd ../../internal/manage && mvn spring-boot:run -Ddb.path=../../../db/

# Frontend (separate terminal)
cd frontend
npm install
npm start
```

Open `http://localhost:4200`
