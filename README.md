# Prediction Market Backend 🚀

A robust, production-grade Spring Boot application powering a real-time prediction market. The system handles sub-second price matching, automated round settlement, and real-time user notifications via WebSocket.

## 🏗 Core Architecture

The backend is designed around high availability and data integrity, featuring:
- **Event-Based Settlement**: Rounds are not time-locked but event-locked. The system monitors live Binance price feeds and settles rounds immediately when a target (UP +3% or DOWN -3%) is hit.
- **Overlap (Continuous) Rounds**: Implements a seamless transition logic ensuring users always have an active round to participate in, eliminating "waiting periods."
- **Server-side Source of Truth**: All time calculations and price validations are performed on the server to prevent client-side manipulation.

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.5.14 (Java 17)
- **Security**: Spring Security + JWT (Stateless)
- **Database**: MySQL 8.0 (Relational data & persistence)
- **Caching & Real-time**: Redis (In-memory price matching & session management)
- **Messaging**: Spring WebSocket + STOMP (Real-time notifications)
- **Mapping**: MapStruct (High-performance DTO mapping)
- **Monitoring**: SLF4J + Logback

## 🌟 Key Features

- **Price Matcher Engine**: An optimized background service that compares real-time ticker data from Binance against all active betting rounds with O(1) lookup efficiency using Redis.
- **Automated Round Lifecycle**:
    - **VOTING**: Open for bets.
    - **LOCKED/MONITORING**: Bets closed, monitoring price movement.
    - **SETTLED**: Result determined and rewards processed.
    - **CANCELLED**: Auto-refund if targets are not hit within 22 hours (Market stagnation protection).
- **Disaster Recovery**: Boot-up routine that re-hydrates active rounds from MySQL to Redis upon server restart to ensure zero-data loss.
- **Concurrency Control**: Physical database constraints and transaction management prevent race conditions like "double betting" or "over-settlement."

## 🚦 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17 SDK
- Maven

### Environment Setup

1. **Infrastructure**: Start MySQL and Redis using Docker:
   ```bash
   docker-compose up -d
   ```
2. **Environment Variables**: Configure your `.env` file based on `.env.example`:
   ```env
   DB_HOST=localhost
   REDIS_HOST=localhost
   JWT_SECRET=your_secure_secret
   ```

### Running the Application

```bash
mvn spring-boot:run
```

## 🔌 API Overview

- `POST /api/v1/auth/register`: User registration.
- `POST /api/v1/auth/login`: Authentication and JWT issuance.
- `GET /api/v1/rounds/current`: Retrieve the active round for betting.
- `POST /api/v1/bets`: Place a prediction (UP or DOWN).
- `GET /api/v1/bets/history`: Paginated user betting history.

## 📡 WebSocket Topics

- `/topic/prices`: Real-time price updates for all pairs.
- `/topic/rounds`: Round status changes and settlement notifications.
- `/user/queue/notifications`: Private notifications for betting outcomes (WIN/LOSE/TIE).
