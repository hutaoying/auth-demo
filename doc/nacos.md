NACOS详解

功能： 服务发现和配置管理
版本：2.2.3


1. 服务发现（Service Discovery）
   Nacos 可以作为服务注册中心，支持：

服务注册与发现（支持临时实例和持久化实例）
健康检查（TCP、HTTP、MySQL 等）
支持多语言 SDK（Java、Go、Python、Node.js 等）
支持主流注册中心协议（Dubbo、gRPC、Spring Cloud）

临时实例 vs 持久化实例
临时实例（Ephemeral）
依赖心跳维持，宕机后自动剔除
无状态服务（如 Web 服务）
持久化实例（Persistent）
注册后永久存在，需手动删除
有状态服务（如数据库、MQ）

默认是临时实例，适合大多数微服务场景。

2. 配置管理（Configuration Management）
   Nacos 提供集中化的配置管理能力：

支持动态配置更新（无需重启服务）
配置版本管理、回滚
灰度发布、多环境（dev/test/prod）隔离
配置监听（Listener）机制
支持 YAML、JSON、Properties、XML 等格式

配置三要素：
Data ID ：配置的唯一标识，通常格式为 服务名-环境.后缀，如 user-service-dev.yaml
Group ：分组，默认是 DEFAULT_GROUP，可用于隔离不同模块
Namespace ：命名空间，用于多租户或环境隔离（如 dev/test/prod）

+----------------+     +----------------+     +----------------+
|   Client App   |<--->|   Nacos Server |<--->|   MySQL        |
| (Spring Cloud) |     | (Cluster 3+节点)|     | (持久化存储)    |
+----------------+     +----------------+     +----------------+
↑
|
+----------------+
|   Nacos Console|
| (Web 管理界面)  |
+----------------+



依赖怎么选：

服务发现依赖
<!-- Spring Cloud Alibaba Nacos Discovery -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>



<!-- Spring Cloud Alibaba Nacos Config -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>

完整的pom.xml示例：

<dependencies>
    <!-- 服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- 配置管理 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2021.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

也可以只使用原生的nacos sdk
<!-- 原生 Nacos 客户端 -->
<dependency>
    <groupId>com.alibaba.nacos</groupId>
    <artifactId>nacos-client</artifactId>
    <version>2.2.3</version>
</dependency>