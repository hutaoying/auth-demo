# 技术设计文档-用户管理模块 角色管理模块 权限管理模块 业务服务扩展模块

## 1. 概述
本技术设计文档详细描述主服务程序的核心模块（用户管理、角色管理、权限管理及业务服务扩展）的实现方案，基于 Spring Boot 3.5.0 和 JDK 17 开发。文档提供类图设计、数据存储设计（包括 ER 图和数据库表设计）、流程图、时序图、接口定义、错误处理和依赖管理，为开发人员提供模块级设计指导。文档聚焦于高层设计，不包含类函数或伪代码，遵循系统架构设计文档中的高层次数据模型和接口定义。数据库采用 PostgreSQL，ORM 框架使用 MyBatis 进行数据库操作。

## 2. 模块设计

### 2.1 用户管理模块
#### 2.1.1 模块概述
负责用户注册、登录、登出及信息管理，采用 Spring Security 实现认证，BCrypt 加密密码，JWT 用于会话管理。登录功能验证用户凭据并生成 JWT 令牌，登出功能通过将 JWT 令牌加入 Redis 黑名单实现令牌失效。数据库操作通过 MyBatis 实现。

#### 2.1.2 类图设计
```plantuml
@startuml
class UserController {
  +register()
  +login()
  +logout()
  +updateUser()
}
class UserService {
  +registerUser()
  +authenticate()
  +logout()
  +updateUserInfo()
}
class UserMapper {
  +findByEmail()
  +insert()
  +update()
}
class UserEntity {
  -id: Long
  -email: String
  -password_hash: String
  -username: String
  -created_at: LocalDateTime
}
class JwtUtil {
  +generateToken()
  +validateToken()
}
class RedisService {
  +addToBlacklist()
  +isBlacklisted()
}
UserController --> UserService
UserService --> UserMapper
UserService --> JwtUtil
UserService --> RedisService
UserMapper --> UserEntity
@enduml
```

#### 2.1.3 数据存储设计
- **数据库表设计**:
  - **Users**:
    - `id`: BIGSERIAL, 主键，自增。
    - `email`: VARCHAR(255), 唯一，非空，邮箱格式。
    - `password_hash`: VARCHAR(255), 非空，BCrypt 加密。
    - `username`: VARCHAR(100), 非空。
    - `created_at`: TIMESTAMP, 非空，默认当前时间。
    - 索引: `email` (唯一索引，优化查询性能)。
- **ER 图**:
```plantuml
@startuml
entity "Users" {
  * id : BIGSERIAL <<PK>>
  --
  email : VARCHAR(255) <<UNIQUE>>
  password_hash : VARCHAR(255)
  username : VARCHAR(100)
  created_at : TIMESTAMP
}
@enduml
```

#### 2.1.4 流程图
- **用户注册**:
```plantuml
@startuml
start
:Receive Register Request;
if (Email format valid?) then (no)
    :Return invalid input error;
    stop
endif
if (Email exists?) then (yes)
    :Return email already exists error;
    stop
else (no)
    :Hash password with BCrypt;
    :Save user to PostgreSQL via MyBatis;
    :Send verification email via Kafka;
    :Return success response;
endif
stop
@enduml
```
- **用户登录**:
```plantuml
@startuml
start
:Receive Login Request;
if (Email format valid?) then (no)
    :Return invalid input error;
    stop
endif
:Query user by email via MyBatis;
if (User exists?) then (no)
    :Return user not found error;
    stop
endif
if (Password matches?) then (no)
    :Return invalid credentials error;
    stop
else (yes)
    :Generate JWT token;
    :Store token in Redis (optional);
    :Return success response with token;
endif
stop
@enduml
```
- **用户登出**:
```plantuml
@startuml
start
:Receive Logout Request;
if (JWT token valid?) then (no)
    :Return invalid token error;
    stop
endif
:Add JWT token to Redis blacklist;
:Return success response;
stop
@enduml
```

#### 2.1.5 时序图
- **用户注册**:
```plantuml
@startuml
actor User
participant "UserController" as Controller
participant "UserService" as Service
participant "UserMapper" as Mapper
participant "Kafka" as Kafka

User -> Controller: POST /api/users/register
Controller -> Service: Process registration
Service -> Mapper: Check email existence
Mapper --> Service: Email not exists
Service -> Service: Hash password
Service -> Mapper: Insert user
Mapper --> Service: Success
Service -> Kafka: Send verification email
Service --> Controller: Success
Controller --> User: HTTP 200, { "status": "success", "user_id": 123 }
@enduml
```
- **用户登录**:
```plantuml
@startuml
actor User
participant "UserController" as Controller
participant "UserService" as Service
participant "UserMapper" as Mapper
participant "JwtUtil" as Jwt
participant "RedisService" as Redis

User -> Controller: POST /api/users/login
Controller -> Service: Process login
Service -> Mapper: Find user by email
Mapper --> Service: User found
Service -> Service: Verify password
Service -> Jwt: Generate JWT token
Jwt --> Service: JWT token
Service -> Redis: Store token (optional)
Redis --> Service: Success
Service --> Controller: Success
Controller --> User: HTTP 200, { "status": "success", "token": "jwt_token" }
@enduml
```
- **用户登出**:
```plantuml
@startuml
actor User
participant "UserController" as Controller
participant "UserService" as Service
participant "JwtUtil" as Jwt
participant "RedisService" as Redis

User -> Controller: POST /api/users/logout
Controller -> Service: Process logout
Service -> Jwt: Validate JWT token
Jwt --> Service: Token valid
Service -> Redis: Add token to blacklist
Redis --> Service: Success
Service --> Controller: Success
Controller --> User: HTTP 200, { "status": "success" }
@enduml
```

#### 2.1.6 接口定义
- **POST /api/users/register**:
  - **Request**: `{ "email": "user@example.com", "password": "pass123", "username": "user1" }`
  - **Response**: `{ "status": "success", "user_id": 123 }`
  - **Description**: 注册新用户，验证邮箱唯一性，保存加密密码，异步发送验证邮件。
- **POST /api/users/login**:
  - **Request**: `{ "email": "user@example.com", "password": "pass123" }`
  - **Response**: `{ "status": "success", "token": "jwt_token" }`
  - **Description**: 用户登录，验证凭据，生成并返回 JWT 令牌，可选地将令牌存储在 Redis。
- **POST /api/users/logout**:
  - **Request**: Header: `Authorization: Bearer jwt_token`
  - **Response**: `{ "status": "success" }`
  - **Description**: 用户登出，验证 JWT 令牌并将其加入 Redis 黑名单以失效，需 JWT 认证。
- **PUT /api/users/{id}**:
  - **Request**: `{ "username": "new_username" }`
  - **Response**: `{ "status": "success" }`
  - **Description**: 更新用户信息，需 JWT 认证，管理员可更新任意用户。

#### 2.1.7 错误处理
- **InvalidInputException**: HTTP 400，提示“邮箱格式无效”或“密码长度不足”。
- **EmailAlreadyExistsException**: HTTP 400，提示“邮箱已注册”。
- **AuthenticationFailureException**: HTTP 401，提示“登录凭据无效”。
- **InvalidTokenException**: HTTP 401，提示“无效或已失效的 JWT 令牌”（适用于登录和登出）。
- **UserNotFoundException**: HTTP 404，提示“用户不存在”（适用于登录）。
- **DatabaseException**: HTTP 500，记录日志并触发重试机制。
- **RedisOperationException**: HTTP 500，提示“令牌黑名单操作失败”（适用于登出）。

#### 2.1.8 依赖管理
- Spring Security 6.3: 用户认证、密码加密和 JWT 管理。
- MyBatis: 数据库操作，映射 SQL 到 Java 对象。
- Spring Kafka: 异步邮件通知。
- Hibernate Validator: 输入验证。
- Redis: 会话存储和 JWT 令牌黑名单管理。
- JJWT (Java JWT): JWT 令牌生成和验证。

### 2.2 角色管理模块
#### 2.2.1 模块概述
实现角色创建和分配功能，基于 RBAC 模型，支持管理员管理角色及其权限。数据库操作通过 MyBatis 实现。

#### 2.2.2 类图设计
```plantuml
@startuml
class RoleController {
  +createRole()
  +assignRole()
}
class RoleService {
  +createRole()
  +assignRole()
}
class RoleMapper {
  +findByName()
  +insert()
  +update()
}
class RoleEntity {
  -id: Long
  -name: String
  -description: String
  -permissions: List<PermissionEntity>
}
class PermissionMapper {
  +findById()
}
RoleController --> RoleService
RoleService --> RoleMapper
RoleService --> PermissionMapper
RoleMapper --> RoleEntity
@enduml
```

#### 2.2.3 数据存储 design
- **数据库表设计**:
  - **Roles**:
    - `id`: BIGSERIAL, 主键，自增。
    - `name`: VARCHAR(50), 唯一，非空。
    - `description`: TEXT, 可空。
    - 索引: `name` (唯一索引，优化查询性能).
  - **User_Roles**:
    - `user_id`: BIGSERIAL, 外键，引用 Users(id).
    - `role_id`: BIGSERIAL, 外键，引用 Roles(id).
    - 主键: (`user_id`, `role_id`).
    - 索引: 复合索引 (`user_id`, `role_id`)，优化查询。
- **ER 图**:
```plantuml
@startuml
entity "Roles" {
  * id : BIGSERIAL <<PK>>
  --
  name : VARCHAR(50) <<UNIQUE>>
  description : TEXT
}
entity "User_Roles" {
  * user_id : BIGSERIAL <<FK>>
  * role_id : BIGSERIAL <<FK>>
}
Roles ||--o{ User_Roles
@enduml
```

#### 2.2.4 流程图 (角色创建)
```plantuml
@startuml
start
:Receive Create Role Request;
if (Role name exists?) then (yes)
    :Return role already exists error;
    stop
else (no)
    :Validate permission IDs;
    if (Permissions valid?) then (no)
        :Return invalid permissions error;
        stop
    endif
    :Save role to PostgreSQL via MyBatis;
    :Return success response;
endif
stop
@enduml
```

#### 2.2.5 时序图 (角色创建)
```plantuml
@startuml
actor Admin
participant "RoleController" as Controller
participant "RoleService" as Service
participant "RoleMapper" as RoleMapper
participant "PermissionMapper" as PermMapper

Admin -> Controller: POST /api/roles
Controller -> Service: Create role
Service -> RoleMapper: Check role name existence
RoleMapper --> Service: Role name not exists
Service -> PermMapper: Validate permission IDs
PermMapper --> Service: Permissions valid
Service -> RoleMapper: Insert role
RoleMapper --> Service: Success
Service --> Controller: Success
Controller --> Admin: HTTP 200, { "status": "success", "role_id": 1 }
@enduml
```