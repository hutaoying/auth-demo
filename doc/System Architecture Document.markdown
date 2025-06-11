# 系统架构设计文档

## 1. 架构概述
系统采用微服务架构，分为主服务和可扩展的业务服务模块。主服务负责用户管理、角色管理和权限管理，通过 Spring Boot 3.5.0 实现，运行在 JDK 17 环境。业务服务以独立模块形式通过 SPI（Service Provider Interface）或 JAR 包动态加载。系统使用 RESTful API 提供服务，结合 Spring Security 实现 RBAC 权限控制。

## 2. 组件视图
### 2.1 主服务
- **用户管理服务**:
  - 功能: 用户注册、登录、信息管理。
  - 技术: Spring Boot REST API, Spring Security。
- **角色管理服务**:
  - 功能: 角色创建、分配。
  - 技术: Spring Data JPA, RBAC 模型。
- **权限管理服务**:
  - 功能: 权限定义、验证。
  - 技术: Spring Security, JWT。
- **服务网关**:
  - 功能: 路由主服务和业务服务请求，统一认证。
  - 技术: Spring Cloud Gateway。

### 2.2 业务服务
- **模块化扩展**:
  - 功能: 动态加载业务服务（如任务管理、订单管理）。
  - 技术: Java SPI, Spring Boot Starter。
- **通信**:
  - 主服务与业务服务通过 REST API 或消息队列交互。

### 2.3 基础设施
- **数据库**: PostgreSQL 16，存储用户、角色、权限数据。
- **消息队列**: Kafka，用于异步事件处理（如邮件通知）。
- **缓存**: Redis，用于会话管理和高频数据缓存。
- **部署**: Docker 容器，Kubernetes 编排。

### 2.4 组件交互
- 用户通过前端（React 或其他）访问服务网关。
- 服务网关验证 JWT 后路由到主服务或业务服务。
- 主服务通过 JPA 访问 PostgreSQL，业务服务可共享或独占数据库。

## 3. 技术选型
- **编程语言**: Java 17 (LTS 版本，性能优化，支持新特性如记录类)。
- **框架**: Spring Boot 3.5.0（支持 GraalVM、AOT 编译，提升启动性能）。
- **认证授权**: Spring Security（RBAC 模型，JWT 认证）。
- **数据库**: PostgreSQL 16（支持 JSONB，适合扩展数据存储）。
- **缓存**: Redis 7（高性能，分布式缓存）。
- **消息队列**: Kafka 3.6（异步事件处理，支持高吞吐）。
- **网关**: Spring Cloud Gateway（路由和过滤）。
- **容器化**: Docker, Kubernetes（部署和扩展）。
- **监控**: Prometheus + Grafana（性能监控）。

## 4. 部署视图
- **部署架构**:
  - 服务网关: 1+ 节点，负载均衡（Nginx 或 Kubernetes Ingress）。
  - 主服务: 2+ 节点，水平扩展。
  - 业务服务: 按需部署，独立容器。
  - 数据库: PostgreSQL 主从集群，读写分离。
  - 缓存/消息队列: Redis 集群，Kafka 分布式部署。
- **环境**:
  - 开发: 本地 Docker Compose。
  - 生产: AWS EKS 或自建 Kubernetes 集群。

## 5. 数据模型
### 5.1 数据库表
- **Users**:
  - `id`: BIGINT, 主键。
  - `email`: VARCHAR(255), 唯一。
  - `password_hash`: VARCHAR(255), BCrypt 加密。
  - `username`: VARCHAR(100).
  - `created_at`: TIMESTAMP.
- **Roles**:
  - `id`: BIGINT, 主键。
  - `name`: VARCHAR(50), 唯一。
  - `description`: TEXT.
- **Permissions**:
  - `id`: BIGINT, 主键。
  - `name`: VARCHAR(50), 唯一。
  - `resource`: VARCHAR(255), 资源路径（如 `/api/tasks`）。
- **User_Roles**:
  - `user_id`: BIGINT, 外键。
  - `role_id`: BIGINT, 外键。
- **Role_Permissions**:
  - `role_id`: BIGINT, 外键。
  - `permission_id`: BIGINT, 外键。
- **Services**:
  - `id`: BIGINT, 主键。
  - `name`: VARCHAR(50), 服务名称。
  - `endpoint`: VARCHAR(255), 服务 API 入口。
  - `metadata`: JSONB, 服务配置。

### 5.2 ER 图
```plantuml
@startuml
entity "Users" {
  * id : BIGINT <<PK>>
  --
  email : VARCHAR(255) <<UNIQUE>>
  password_hash : VARCHAR(255)
  username : VARCHAR(100)
  created_at : TIMESTAMP
}

entity "Roles" {
  * id : BIGINT <<PK>>
  --
  name : VARCHAR(50) <<UNIQUE>>
  description : TEXT
}

entity "Permissions" {
  * id : BIGINT <<PK>>
  --
  name : VARCHAR(50) <<UNIQUE>>
  resource : VARCHAR(255)
}

entity "User_Roles" {
  * user_id : BIGINT <<FK>>
  * role_id : BIGINT <<FK>>
}

entity "Role_Permissions" {
  * role_id : BIGINT <<FK>>
  * permission_id : BIGINT <<FK>>
}

entity "Services" {
  * id : BIGINT <<PK>>
  --
  name : VARCHAR(50)
  endpoint : VARCHAR(255)
  metadata : JSONB
}

Users ||--o{ User_Roles
Roles ||--o{ User_Roles
Roles ||--o{ Role_Permissions
Permissions ||--o{ Role_Permissions
@enduml
```

## 6. 接口定义
- **用户管理**:
  - `POST /api/users/register`: 注册用户。
    - Request: `{ "email": "user@example.com", "password": "pass123", "username": "user1" }`
    - Response: `{ "status": "success", "user_id": 123 }`
  - `POST /api/users/login`: 用户登录。
    - Request: `{ "email": "user@example.com", "password": "pass123" }`
    - Response: `{ "token": "jwt_token" }`
- **角色管理**:
  - `POST /api/roles`: 创建角色。
    - Request: `{ "name": "admin", "description": "Administrator role", "permissions": [1, 2] }`
    - Response: `{ "status": "success", "role_id": 1 }`
- **权限管理**:
  - `POST /api/permissions`: 定义权限。
    - Request: `{ "name": "task:read", "resource": "/api/tasks" }`
    - Response: `{ "status": "success", "permission_id": 1 }`
- **业务服务**:
  - `POST /api/services/register`: 注册业务服务。
    - Request: `{ "name": "task-service", "endpoint": "/api/tasks", "metadata": {} }`
    - Response: `{ "status": "success", "service_id": 1 }`

## 7. 非功能实现策略
- **性能**:
  - 使用 Redis 缓存用户会话和角色权限。
  - 数据库索引优化查询性能。
- **安全性**:
  - Spring Security 实现 JWT 认证，BCrypt 加密密码。
  - API 使用 HTTPS，防止中间人攻击。
- **可扩展性**:
  - 使用 Java SPI 加载业务服务模块。
  - Kubernetes 支持动态扩展服务实例。
- **可用性**:
  - 主服务和数据库部署多节点，故障转移。
  - 使用健康检查（Spring Actuator）监控服务状态。