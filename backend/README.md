# Books Quest Backend

Spring Boot microservices backend for the books quest application.

## Modules

- **shared** - Shared config, DTOs, and exceptions
- **api/auth** - Authentication service
- **api/book** - Book catalog service
- **api/game** - Game and session service
- **internal/manage** - Internal book management

## Ports

- Auth service: `9092`
- Book service: `9090`
- Game service: `9091`

## Build

```bash
mvn clean install
```

## Run

Run each service in its own terminal:

```bash
cd api/auth
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd api/book
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd api/game
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/

cd internal/manage
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=../../../db/
```

## Database

SQLite databases stored in `/db/` at project root.

Config files in `shared/src/main/resources/`:
- `shared-common.properties` - Common settings
- `shared-dev.properties` - Development (uses `books-quest-dev.db`)
- `shared-prod.properties` - Production (uses `books-quest-prod.db`)

Override database path:
```bash
mvn test spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" -Ddb.path=/custom/path/
```

## API Endpoints

### Book Service (`/api/books`)
- `GET /` - List all books
- `GET /{id}` - Get book by ID
- `GET /search` - Search books
- `GET /filter` - Filter books

### Game Service (`/api/games`)
- `POST /start/{bookId}` - Start new game
- `GET /{gameSessionId}` - Get game session
- `POST /{gameSessionId}/move` - Make a move
- `POST /{gameSessionId}/save` - Save progress

### Auth Service (`/api/auth`)
- Authentication and authorization endpoints

### Manage Service
- Internal book management (no public endpoints)
