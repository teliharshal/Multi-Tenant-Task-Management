# Multi-Tenant Task Management System

A **Spring Boot backend application** for managing tasks in a **multi-tenant SaaS environment** using a **single database** and **single codebase**, while enforcing **strict tenant isolation**.

Each organization (**tenant**) has its own users and tasks, and **no tenant can access another tenant’s data**.

---

## Table of Contents

* [Overview](#overview)
* [Assignment Requirements Covered](#assignment-requirements-covered)
* [Features](#features)
* [Tech Stack](#tech-stack)
* [Architecture & Multi-Tenant Design](#architecture--multi-tenant-design)
* [Entities](#entities)
* [Roles & Permissions](#roles--permissions)
* [Tenant Isolation Strategy](#tenant-isolation-strategy)
* [JWT Structure](#jwt-structure)
* [Security Request Flow](#security-request-flow)
* [Project Structure](#project-structure)
* [API Documentation](#api-documentation)
* [Validation & Exception Handling](#validation--exception-handling)
* [Database Design Rules](#database-design-rules)
* [How to Run the Project](#how-to-run-the-project)
* [Testing](#testing)
* [SonarQube Integration](#sonarqube-integration)
* [Sample End-to-End Workflow](#sample-end-to-end-workflow)
* [Important Multi-Tenant Protection Rules](#important-multi-tenant-protection-rules)
* [Future Improvements](#future-improvements)

---

# Overview

This project implements a **Multi-Tenant Task Management System** where:

* Multiple tenants share the **same backend application**
* All tenant data is stored in a **single database**
* Tenant data is isolated using **tenant-aware JWT authentication**
* Tenant information is **never accepted from request body or query parameters**
* Tenant is resolved **only from JWT**
* All secured APIs enforce **tenant-level access control**

This assignment focuses on:

* **Spring Security fundamentals**
* **JWT handling and customization**
* **OncePerRequestFilter usage**
* **Tenant isolation logic**
* **Repository-level data protection**

---

# Assignment Requirements Covered

This project satisfies the core requirements of the assignment:

* Multi-tenant backend using **single database**
* **Tenant registration** with first **ADMIN** user creation
* **JWT authentication** with `tenantId`, `userId`, and `role`
* **Tenant resolved only from JWT**
* `tenant_id` is **never accepted** in request body or query params
* **Cross-tenant access blocked** at repository/service level
* Role-based access control for **ADMIN / MANAGER / USER**
* **OncePerRequestFilter** used to extract JWT claims and set tenant context
* **Tenant-aware repository methods** for all secured data access
* **Global exception handling**
* **Request payload validation**
* **JUnit test cases** for controllers/services
* Targeted **80%+ code coverage**
* **SonarQube integration**
* README includes:

    * tenant isolation explanation
    * JWT structure and claims
    * security filter flow
    * example API documentation
    * application run steps

---

# Features

* Register a new **tenant** with its first **ADMIN**
* Login with **JWT-based authentication**
* Role-based authorization using:

    * `ADMIN`
    * `MANAGER`
    * `USER`
* Create users under the same tenant
* Create tasks within the current tenant
* Assign tasks only to users of the same tenant
* Update task status by the assigned user only
* Fetch tasks based on role and tenant restrictions
* Centralized exception handling
* Request validation with meaningful error responses
* Unit and integration testing
* SonarQube support for static analysis and code quality

---

# Tech Stack

* **Java 17**
* **Spring Boot 3**
* **Spring Security**
* **Spring Data JPA**
* **H2 Database**
* **JWT (JJWT)**
* **JUnit 5**
* **MockMvc**
* **Maven**
* **SonarQube**

---

# Architecture & Multi-Tenant Design

## Core Idea

This is a **single-database multi-tenant system**.

All tenants share:

* the **same application**
* the **same database**
* the **same tables**

Tenant isolation is achieved by attaching every tenant-owned record to a `tenant_id` and ensuring that **all authenticated operations resolve the tenant from JWT only**.

That means:

* No API accepts `tenant_id` from client input
* No repository query is allowed to fetch cross-tenant data
* No update is allowed unless the entity belongs to the authenticated tenant

---

# Entities

## 1) Tenant

| Field     | Type          |
| --------- | ------------- |
| id        | Long          |
| name      | String        |
| createdAt | LocalDateTime |

## 2) User

| Field    | Type                              |
| -------- | --------------------------------- |
| id       | Long                              |
| email    | String                            |
| password | String                            |
| role     | Enum (`ADMIN`, `MANAGER`, `USER`) |
| tenant   | Tenant                            |

## 3) Task

| Field       | Type                                         |
| ----------- | -------------------------------------------- |
| id          | Long                                         |
| title       | String                                       |
| description | String                                       |
| status      | Enum (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| assignedTo  | User                                         |
| tenant      | Tenant                                       |

---

# Roles & Permissions

## ADMIN

* Can create users within the same tenant
* Can view all tasks of the tenant

## MANAGER

* Can create tasks
* Can assign tasks to users of the same tenant
* Can view all tasks of the tenant

## USER

* Can view only tasks assigned to them
* Can update status of only their assigned tasks

---

# Tenant Isolation Strategy

Tenant isolation is the most critical part of this project.

## How isolation is achieved

### 1) `tenantId` is stored inside JWT

When a user logs in, the generated JWT contains:

* `userId`
* `tenantId`
* `role`

This ensures the tenant context comes from the authenticated token, not from client input.

---

### 2) Tenant is resolved only from JWT

The system **never accepts `tenant_id` from request body, request params, or path variables**.

Example of what is **not allowed**:

* Passing `tenantId` in task creation request
* Passing `tenantId` in user creation request
* Passing `tenantId` in query params to fetch data

Instead, the application always does this internally:

```java
Long tenantId = TenantContext.getTenantId();
```

---

### 3) `OncePerRequestFilter` extracts JWT details

For every secured request:

* JWT is read from `Authorization: Bearer <token>`
* Token is validated
* `tenantId` is extracted from JWT
* `userId` is extracted from JWT
* `role` is extracted from JWT
* These values are stored in request context / security context for the current request lifecycle

---

### 4) Service layer always uses tenant from context

Business logic never trusts tenant information from the client.

Example:

```java
Long tenantId = TenantContext.getTenantId();
Long userId = SecurityUtils.getCurrentUserId();
String role = SecurityUtils.getCurrentUserRole();
```

---

### 5) Repository methods are tenant-aware

All repository methods enforce tenant filtering.

Examples:

```java
findByIdAndTenantId(id, tenantId)
findAllByTenantId(tenantId)
findByIdAndTenantId(userId, tenantId)
findAllByTenantIdAndAssignedToId(tenantId, userId)
```

This ensures even if someone guesses another tenant’s entity ID, the query still returns nothing unless it belongs to the authenticated tenant.

---

### 6) Cross-tenant updates are blocked

All update operations first verify that the entity belongs to the current tenant.

Examples:

* A manager cannot assign a task from another tenant
* A user cannot update another tenant’s task
* An admin cannot create or manage users in another tenant

---

# JWT Structure

## JWT Claims

The JWT contains:

* `sub` → user email
* `userId` → logged-in user ID
* `tenantId` → current tenant ID
* `role` → current user role

## Example JWT Payload

```json
{
  "sub": "manager@acme.com",
  "userId": 2,
  "tenantId": 1,
  "role": "MANAGER",
  "iat": 1720100000,
  "exp": 1720103600
}
```

---

# Security Request Flow

## Request Flow

### 1. Client sends request with JWT

```http
Authorization: Bearer <token>
```

### 2. JWT filter runs for secured endpoints

The filter:

* validates the token
* extracts `userId`
* extracts `tenantId`
* extracts `role`
* extracts username/email

### 3. Tenant context is stored

The application stores current request tenant/user data in a `TenantContext` / `SecurityContext`.

### 4. Spring Security authenticates the request

If token is valid, the request proceeds with the authenticated user.

### 5. Controller calls service layer

The service layer uses the current request tenant and user info from context.

### 6. Repository queries fetch only current tenant data

All database queries include tenant filtering, so data from another tenant can never be returned.

---

# Project Structure

```bash
src/main/java/com/example/taskmanagement
│
├── controller         # REST controllers
├── service            # business logic
├── repository         # JPA repositories
├── entity             # JPA entities
├── dto
│   ├── request        # request DTOs
│   └── response       # response DTOs
├── security           # JWT, filter, tenant context, security config
├── exception          # custom exceptions + global exception handler
└── config             # data initializer / test data setup
```

---

# API Documentation

## Base URL

```bash
http://localhost:8080
```

---

## 1) Register Tenant

### Endpoint

```http
POST /tenant/register
```

### Description

Creates a new tenant and creates the first **ADMIN** user for that tenant.

### Access

**Public**

### Request Body

```json
{
  "tenantName": "Acme Corp",
  "adminEmail": "admin@acme.com",
  "adminPassword": "password123"
}
```

### Success Response — `201 Created`

```json
{
  "tenantId": 1,
  "tenantName": "Acme Corp",
  "adminId": 1,
  "adminEmail": "admin@acme.com",
  "role": "ADMIN",
  "message": "Tenant registered successfully"
}
```

### Validation Error — `400 Bad Request`

```json
{
  "tenantName": "Tenant name is required"
}
```

or

```json
{
  "adminEmail": "Email should be valid"
}
```

---

## 2) Login

### Endpoint

```http
POST /auth/login
```

### Description

Authenticates a user and returns a JWT containing tenant information and role.

### Access

**Public**

### Request Body

```json
{
  "email": "admin@acme.com",
  "password": "password123"
}
```

### Success Response — `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Invalid Credentials — `401 Unauthorized`

```json
{
  "message": "Invalid email or password"
}
```

---

## 3) Create User

### Endpoint

```http
POST /users
```

### Access

**ADMIN only**

### Description

Creates a new user under the same tenant as the logged-in ADMIN.

### Headers

```http
Authorization: Bearer <ADMIN_JWT>
```

### Request Body

```json
{
  "email": "manager@acme.com",
  "password": "password123",
  "role": "MANAGER"
}
```

### Success Response — `201 Created`

```json
{
  "id": 2,
  "email": "manager@acme.com",
  "role": "MANAGER",
  "tenantId": 1
}
```

### Forbidden — `403 Forbidden`

```json
{
  "message": "Access Denied"
}
```

### Validation Error — `400 Bad Request`

```json
{
  "role": "Role is required"
}
```

---

## 4) Create Task

### Endpoint

```http
POST /tasks
```

### Access

**MANAGER only**

### Description

Creates a new task under the tenant extracted from JWT.

### Headers

```http
Authorization: Bearer <MANAGER_JWT>
```

### Request Body

```json
{
  "title": "Prepare monthly report",
  "description": "Prepare finance and sales report"
}
```

### Success Response — `201 Created`

```json
{
  "id": 1,
  "title": "Prepare monthly report",
  "description": "Prepare finance and sales report",
  "status": "PENDING",
  "assignedToUserId": null,
  "assignedToUserEmail": null,
  "tenantId": 1
}
```

### Forbidden — `403 Forbidden`

```json
{
  "message": "Access Denied"
}
```

---

## 5) Assign Task

### Endpoint

```http
PUT /tasks/{id}/assign
```

### Access

**MANAGER only**

### Description

Assigns an existing task to a user within the same tenant only.

### Headers

```http
Authorization: Bearer <MANAGER_JWT>
```

### Path Variable

* `id` → task ID

### Request Body

```json
{
  "userId": 3
}
```

### Success Response — `200 OK`

```json
{
  "id": 1,
  "title": "Prepare monthly report",
  "description": "Prepare finance and sales report",
  "status": "PENDING",
  "assignedToUserId": 3,
  "assignedToUserEmail": "user@acme.com",
  "tenantId": 1
}
```

### If Task Not Found in Current Tenant — `404 Not Found`

```json
{
  "message": "Task not found for current tenant"
}
```

### If User Not Found in Current Tenant — `404 Not Found`

```json
{
  "message": "User not found for current tenant"
}
```

### If Assigned User Role Is Not USER — `403 Forbidden`

```json
{
  "message": "Task can only be assigned to a USER"
}
```

---

## 6) Update Task Status

### Endpoint

```http
PUT /tasks/{id}/status
```

### Access

**USER only**

### Description

Allows a user to update the status of only tasks assigned to them.

### Headers

```http
Authorization: Bearer <USER_JWT>
```

### Path Variable

* `id` → task ID

### Request Body

```json
{
  "status": "COMPLETED"
}
```

### Success Response — `200 OK`

```json
{
  "id": 1,
  "title": "Prepare monthly report",
  "description": "Prepare finance and sales report",
  "status": "COMPLETED",
  "assignedToUserId": 3,
  "assignedToUserEmail": "user@acme.com",
  "tenantId": 1
}
```

### If Task Not Found in Current Tenant — `404 Not Found`

```json
{
  "message": "Task not found for current tenant"
}
```

### If Task Is Not Assigned To Current User — `403 Forbidden`

```json
{
  "message": "You can update only tasks assigned to you"
}
```

---

## 7) Get Tasks

### Endpoint

```http
GET /tasks
```

### Access

**ADMIN / MANAGER / USER**

### Description

Returns tasks based on role:

* **ADMIN / MANAGER** → all tasks in their tenant
* **USER** → only tasks assigned to that user

### Headers

```http
Authorization: Bearer <JWT>
```

### Success Response for ADMIN / MANAGER — `200 OK`

```json
[
  {
    "id": 1,
    "title": "Prepare monthly report",
    "description": "Prepare finance and sales report",
    "status": "PENDING",
    "assignedToUserId": 3,
    "assignedToUserEmail": "user@acme.com",
    "tenantId": 1
  },
  {
    "id": 2,
    "title": "Create dashboard",
    "description": "Build tenant analytics dashboard",
    "status": "IN_PROGRESS",
    "assignedToUserId": 4,
    "assignedToUserEmail": "employee@acme.com",
    "tenantId": 1
  }
]
```

### Success Response for USER — `200 OK`

```json
[
  {
    "id": 1,
    "title": "Prepare monthly report",
    "description": "Prepare finance and sales report",
    "status": "PENDING",
    "assignedToUserId": 3,
    "assignedToUserEmail": "user@acme.com",
    "tenantId": 1
  }
]
```

---

# API Summary Table

| Method | Endpoint             | Access                 | Description                           |
| ------ | -------------------- | ---------------------- | ------------------------------------- |
| POST   | `/tenant/register`   | Public                 | Register tenant and first admin       |
| POST   | `/auth/login`        | Public                 | Login and get JWT                     |
| POST   | `/users`             | ADMIN                  | Create user in same tenant            |
| POST   | `/tasks`             | MANAGER                | Create task                           |
| PUT    | `/tasks/{id}/assign` | MANAGER                | Assign task to same-tenant user       |
| PUT    | `/tasks/{id}/status` | USER                   | Update own assigned task status       |
| GET    | `/tasks`             | ADMIN / MANAGER / USER | Get tenant tasks / own assigned tasks |

---

# Security Rules Implemented

* JWT contains `tenantId` and `role`
* Tenant is resolved **only from JWT**
* `tenant_id` is **never accepted from request body**
* All secured APIs require authentication
* Cross-tenant access is blocked
* Repository methods always filter by `tenantId`
* User can update only their own assigned task
* Manager can assign only to users of the same tenant
* Admin creates users only within their own tenant

---

# Example Authorization Header

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

# Validation & Exception Handling

The project uses:

* `@Valid` on request DTOs
* Bean validation annotations such as:

    * `@NotBlank`
    * `@Email`
    * `@NotNull`
* Global exception handling using `@RestControllerAdvice`
* Custom exceptions such as:

    * `ResourceNotFoundException`
    * `UnauthorizedException`
    * `ForbiddenOperationException`
    * `DuplicateResourceException`

## Common Error Responses

### 400 Bad Request

```json
{
  "email": "must be a well-formed email address"
}
```

### 401 Unauthorized

```json
{
  "message": "Invalid email or password"
}
```

### 403 Forbidden

```json
{
  "message": "Access Denied"
}
```

### 404 Not Found

```json
{
  "message": "Task not found for current tenant"
}
```

---

# Database Design Rules

* Single **H2 database**
* All tenant-owned tables include `tenant_id`
* No separate database per tenant
* No separate schema per tenant
* Repository access is always tenant-aware

---

# How to Run the Project

## 1) Clone the repository

```bash
git clone <your-github-repo-url>
cd Multi-Tenant-Task-Management
```

## 2) Build the project

```bash
mvn clean install
```

## 3) Run the application

```bash
mvn spring-boot:run
```

Application will start at:

```bash
http://localhost:8080
```

---

# H2 Database Console

If enabled in `application.properties`:

```bash
http://localhost:8080/h2-console
```

### Example H2 Settings

* **JDBC URL:** `jdbc:h2:mem:testdb`
* **Username:** `sa`
* **Password:** *(empty if configured empty)*

---

# Testing

The project includes:

* **Controller tests**
* **Service tests**
* **Authentication / JWT tests**
* **Tenant isolation tests**
* **Repository behavior tests**
* **Security access tests**

## Run Tests

```bash
mvn test
```

## Coverage Goal

* Minimum **80% code coverage**

Recommended test areas:

* Tenant registration
* Login
* Create user
* Create task
* Assign task
* Update task status
* Get tasks
* Unauthorized access
* Cross-tenant access denial
* Validation failures

---

# SonarQube Integration

This project supports **SonarQube** for code quality and static analysis.

## Run SonarQube Analysis

```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=multi-tenant-task-management \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_SONAR_TOKEN
```

## SonarQube Checks

* Code smells
* Bugs
* Vulnerabilities
* Duplicated code
* Test coverage
* Maintainability issues

---

# Sample End-to-End Workflow

## Step 1: Register tenant

```http
POST /tenant/register
```

## Step 2: Login with admin

```http
POST /auth/login
```

## Step 3: Create manager/user using admin token

```http
POST /users
```

## Step 4: Login as manager

```http
POST /auth/login
```

## Step 5: Create task

```http
POST /tasks
```

## Step 6: Assign task to user

```http
PUT /tasks/{id}/assign
```

## Step 7: Login as user

```http
POST /auth/login
```

## Step 8: Update assigned task status

```http
PUT /tasks/{id}/status
```

## Step 9: Fetch tasks

```http
GET /tasks
```

---

# Important Multi-Tenant Protection Rules

This project ensures that:

* Tenant A user **cannot fetch** Tenant B tasks
* Tenant A manager **cannot assign** tasks to Tenant B user
* Tenant A admin **cannot create** users in Tenant B
* Tenant A user **cannot update** Tenant B task
* Tenant A user **cannot view all tasks** of another tenant
* Even if an ID is guessed manually, repository-level tenant filtering prevents access

---

# Future Improvements

* Refresh token support
* Swagger / OpenAPI documentation
* Pagination and filtering for tasks
* Audit logging
* Docker support
* PostgreSQL profile
* Tenant-specific reporting dashboard

---

# Notes

* This is a **backend-focused assignment**
* UI is **not required**
* Tenant isolation is the primary design goal of the application
* All secured operations must be validated against the authenticated tenant context
