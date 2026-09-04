# Personal Finance Manager

A backend RESTful API service for personal finance management built with Java 17, Spring Boot 3, Spring Security, Spring Data JPA, and an in-memory H2 database.

---

## 1. Requirements

- **Java Development Kit (JDK)**: Java 17 or later
- **Build Tool**: Maven 3.8+ (or the included Maven wrapper `./mvnw`)

---

## 2. Setup and Execution

### Build Executable Package
```bash
./mvnw clean package
```

### Run Locally
```bash
./mvnw spring-boot:run
```
Alternatively, run the packaged JAR:
```bash
java -jar target/personal-finance-manager-0.0.1-SNAPSHOT.jar
```

The application starts by default on `http://localhost:8080`.

---

## 3. Testing and Code Coverage

### Run Test Suite
```bash
./mvnw test
```

### Generate Coverage Report
```bash
./mvnw test jacoco:report
```
The JaCoCo HTML report is generated at `target/site/jacoco/index.html`.

**Coverage Metrics Achieved:**
- Line Coverage: > 98%
- Instruction Coverage: > 98%
- Branch Coverage: > 81%
- Total Automated Tests: 132 tests (100% passing)

### Run Provided End-to-End Test Script
Make the script executable and run against the running server:
```bash
chmod +x ./financial_manager_tests.sh
./financial_manager_tests.sh "http://localhost:8080/api"
```
*(All 86/86 tests across all 8 scenarios pass with 100% success rate)*

---

## 4. API Specification Summary

All endpoints consume and produce `application/json`. Authenticated endpoints require a valid `JSESSIONID` cookie obtained from the login endpoint.

### Authentication Endpoints
| Method | Endpoint | Description | Success Status | Error Statuses |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user | `201 Created` | `400`, `409` |
| `POST` | `/api/auth/login` | Authenticate user and initiate session | `200 OK` | `400`, `401` |
| `POST` | `/api/auth/logout` | Terminate session | `200 OK` | `401` |

### Category Endpoints
| Method | Endpoint | Description | Success Status | Error Statuses |
|---|---|---|---|---|
| `GET` | `/api/categories` | Retrieve all default and user custom categories | `200 OK` | `401` |
| `POST` | `/api/categories` | Create a custom category for the user | `201 Created` | `400`, `401`, `409` |
| `DELETE` | `/api/categories/{name}` | Delete a user custom category (if unused) | `200 OK` | `400`, `401`, `403`, `404` |

### Transaction Endpoints
| Method | Endpoint | Description | Success Status | Error Statuses |
|---|---|---|---|---|
| `POST` | `/api/transactions` | Record a new transaction | `201 Created` | `400`, `401` |
| `GET` | `/api/transactions` | Retrieve user transactions (supports filters and date DESC sorting) | `200 OK` | `400`, `401` |
| `PUT` | `/api/transactions/{id}` | Update an existing transaction (date is immutable) | `200 OK` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/transactions/{id}` | Delete an existing transaction | `200 OK` | `401`, `403`, `404` |

### Savings Goal Endpoints
| Method | Endpoint | Description | Success Status | Error Statuses |
|---|---|---|---|---|
| `POST` | `/api/goals` | Create a new savings goal with target date and amount | `201 Created` | `400`, `401` |
| `GET` | `/api/goals` | List all savings goals for the user with calculated progress | `200 OK` | `401` |
| `GET` | `/api/goals/{id}` | Retrieve a specific savings goal by ID | `200 OK` | `401`, `403`, `404` |
| `PUT` | `/api/goals/{id}` | Update savings goal parameters | `200 OK` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/goals/{id}` | Delete a savings goal | `200 OK` | `401`, `403`, `404` |

### Financial Reports Endpoints
| Method | Endpoint | Description | Success Status | Error Statuses |
|---|---|---|---|---|
| `GET` | `/api/reports/monthly/{year}/{month}` | Generate monthly category breakdown and net savings | `200 OK` | `400`, `401` |
| `GET` | `/api/reports/yearly/{year}` | Generate yearly category breakdown and net savings | `200 OK` | `400`, `401` |

---

## 5. Configuration

Configuration properties in `src/main/resources/application.properties`:

- **Database**: In-memory H2 database (`jdbc:h2:mem:financedb`).
- **Session Management**: Session cookies configured with `HttpOnly` and `SameSite=Strict`.
- **Port Overrides**: Pass `-Dserver.port=<port>` or environment variable `SERVER_PORT=<port>`.

---

## 6. Deployment Information

The service packages into a self-contained executable JAR file containing all runtime dependencies and an embedded Tomcat server.

### Running with Custom Port and Environment Variables
```bash
java -Dserver.port=8080 -jar target/personal-finance-manager-0.0.1-SNAPSHOT.jar
```

---

## 7. Key Design Decisions

1. **Session-Based Authentication**:
   - Implemented standard cookie-based sessions (`JSESSIONID`) via Spring Security.
   - Explicit JSON error responses (`401 Unauthorized` and `403 Forbidden`) configured in custom security entry points and access denied handlers.

2. **Strict User Data Isolation**:
   - All transactions, goals, and custom categories are bound to the authenticated user ID.
   - Cross-user queries or modifications return `403 Forbidden` or `404 Not Found`.

3. **Dynamic Goal Calculations**:
   - Savings goal progress is evaluated dynamically: `currentProgress = sum(income) - sum(expenses)` for all active transactions on or after the goal's `startDate`.
   - Deleted transactions are automatically excluded from goal progress and reports.

4. **Default Category Seeding & Protection**:
   - Standard default categories (`Salary`, `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities`) are seeded on application initialization.
   - Deletion of default categories and categories referenced by existing transactions is blocked with `400 Bad Request`.

5. **Uniform Error Structure**:
   - All client and server-side errors produce standard JSON payloads:
     ```json
     {
       "message": "Descriptive error message"
     }
     ```
