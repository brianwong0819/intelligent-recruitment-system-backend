# 🚀 Intelligent Event Recruitment Platform (Backend)

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.X-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.X-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

## 📌 Project Overview

This repository contains the Backend architecture for the Intelligent Event Recruitment Platform. It serves as the secure, high-performance engine that bridges event organizers with freelance professionals. Built to handle complex scheduling, geolocation matching, and role-based data isolation, this API dynamically orchestrates the recruitment lifecycle. By deeply integrating Google Gemini AI, it automates heavy-lifting tasks such as candidate evaluation and training material generation, drastically reducing manual recruiter overhead.

## 🏗️ System Architecture & Tech Stack

The system is designed using a strict layered architecture (Controller, Service, Repository, DTO) to ensure robust separation of concerns, high testability, and seamless API contract management.

*   **Core Framework**: Java 17+ with Spring Boot 3.x for enterprise-grade dependency injection and auto-configuration.
*   **Data Persistence**: Spring Data JPA / Hibernate mapping to a relational database, utilizing advanced JPQL and Specifications for dynamic querying.
*   **Security & Authentication**: Spring Security fortified with stateless JSON Web Tokens (JWT) and persistent Refresh Token rotation for secure, long-lived sessions.
*   **AI Micro-processing**: A hybrid architecture where the Java backend orchestrates Python 3.x scripts (`google-generativeai`) via `ProcessBuilder` to process PDFs and evaluate candidates using Gemini AI.
*   **Notification Engine**: Automated email dispatching driven by Spring Mail and Thymeleaf HTML templates for dynamic, data-rich communications.

## ✨ Key Features

### 🤖 AI-Driven Recruitment Engine
*   **Candidate Evaluation**: The system pipes applicant data and resumes to Gemini AI, returning structured JSON scoring (Experience, Skills, Location, Availability) to objectively rank applicants.
*   **Automated Training Quizzes**: Analyzes recruiter-uploaded PDF training materials and dynamically generates role-specific multiple-choice assessments.

### 🔐 Security & Identity Management
*   **Multi-Role Access Control**: Distinct authentication strategies and endpoint security rules for Candidates, Recruiters, and System Administrators (`@PreAuthorize` directives).
*   **Token Rotation**: Implements a secure `RefreshTokenService` to gracefully handle session extensions without compromising the primary JWT lifecycle.

### 💼 Core Business Logic
*   **Complex Job Scheduling**: Supports the creation of multi-day, multi-location job postings with granular tracking of positions needed vs. positions filled per location.
*   **Geospatial Talent Matching**: Integrates Google Maps APIs and utilizes bounding-box/radius logic to surface jobs near a candidate's preferred location.
*   **Automated Reputation & Reminders**: A chron-job scheduled `JobReminderService` automatically dispatches instructions to hired candidates 48 hours before an event, while a reputation engine penalizes late cancellations.

## 🚀 Getting Started (Developer Guide)

### Prerequisites
*   **Java**: JDK 17 or higher.
*   **Python**: v3.8+ (Required for Gemini AI script execution). Ensure `google-generativeai` is installed.
*   **Database**: MySQL or PostgreSQL instance running locally.
*   **Maven**: Embedded wrapper provided (`mvnw`).

### Installation & Setup

1.  Clone the repository and build the project:
    ```bash
    ./mvnw clean install -DskipTests
    ```

### Environment Variables

The application requires specific properties to be defined. Create an `application-local.properties` (or set environment variables) based on the provided configuration files:

| Key | Description | Example |
| :--- | :--- | :--- |
| `spring.datasource.url` | Database connection string | `jdbc:mysql://localhost:3306/recruitment_db` |
| `jwt.secret` | Base64 encoded secret for JWT signing | `your_super_secret_key_here` |
| `gemini.ai.api-key` | Google Gemini API Key | `AIzaSy...` |
| `gemini.ai.python-command`| Path to python executable | `python3` (or `python`) |

*Note: Ensure your `gemini-ai.properties` and `training-quiz.properties` are properly populated with valid API keys and script paths.*

### Running the Application

Execute the Spring Boot application via the Maven wrapper:
```bash
./mvnw spring-boot:run
```

## 📁 Project Structure (Abridged)

```text
├── scripts/                            # Python scripts for Gemini AI integration
│   ├── candidate_evaluation.py
│   └── generate_training_quiz.py
├── src/main/java/com/.../
│   ├── config/                         # App configs (CORS, Security, AI, Maps)
│   ├── controller/                     # REST API endpoints (Admin, Auth, Job, Recruiter)
│   ├── dto/                            # Data Transfer Objects for strict API contracts
│   ├── model/                          # JPA Entities and Enums representing the domain
│   ├── repository/                     # Spring Data JPA Interfaces & dynamic Specifications
│   ├── security/                       # JWT filters, token utilities, auth services
│   ├── service/                        # Core business logic and transaction management
│   └── util/                           # Mappers for Entity <-> DTO conversions
└── src/main/resources/
    ├── templates/email/                # Thymeleaf HTML email templates
    ├── application.properties          # Primary Spring Boot configuration
    └── gemini-ai.properties            # Dedicated AI configuration constraints
```

## 🔌 API Endpoints Overview

The API is fully RESTful. Key resource domains include:

*   **`POST /api/auth/*`**: Unified authentication endpoints returning JWTs and Refresh Tokens for all user roles.
*   **`GET /api/recruiters/candidates/search`**: Advanced search utilizing `CandidateSpecifications` to filter by availability, demographics, skills, and languages.
*   **`POST /api/ai/evaluate/*`**: Triggers asynchronous Python worker processes to generate candidate compatibility scores.
*   **`POST /api/job-schedules`**: Handles the complex payload structures required for assigning specific dates, times, and manpower requirements to geolocations.
*   **`POST /api/training/quiz/generate`**: Uploads a PDF to Gemini AI and returns a structured JSON assessment.

## 🔮 Future Improvements

1.  **Asynchronous Event Queues**: Migrate the `EmailService` and `GeminiAIService` executions from synchronous blocking calls to an async message broker (e.g., RabbitMQ or Kafka) to improve API response times under high load.
2.  **Caching Layer**: Introduce Redis via Spring Cache to store frequently accessed but rarely mutated data, such as public recruiter portfolios and location geocoding results.
3.  **Containerization**: Add a `Dockerfile` and `docker-compose.yml` to package the Java application, the Python environment, and the database into a unified, reproducible deployment environment.
