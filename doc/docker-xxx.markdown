启动postgresql 容器
verion: 14
docker pull postgresql:14
docker run -d \
  --name 1-postgres-1 \
  -e POSTGRES_USER=root \
  -e POSTGRES_PASSWORD='1qazcde3!@#' \
  -e POSTGRES_DB=ailpha \
  -p 5432:5432 \
  -v pg_data:/var/lib/postgresql/data \
  postgres:14


----  redis ------
version:8.0.2

  -v /data/redis/config/redis.conf:/etc/redis/redis.conf \

  docker run -d \
    --name 1-redis-1 \
    --restart unless-stopped \
    --memory 4g \
    --cpus 2 \
    --ulimit nofile=65535:65535 \
    -p 6379:6379 \
    -v redis_data:/data \
    -v redis_logs:/logs \
    redis:8.0.2 \
    redis-server /etc/redis/redis.conf \
    --requirepass "2wsxVFR_" \
    --appendonly yes \
    --save "" \
    --maxmemory 3gb \
    --maxmemory-policy volatile-lru


### kafka 启动

version:


### Nacos-mysql
docker pull mysql:8.0.36

docker run -d --name mysql8-nacos \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=admin \
  -e TZ=Asia/Shanghai \
  -v /Users/taoyinghu/Docker/data/mysql/data:/var/lib/mysql \
  mysql:8.0.36

 wget https://xxx/xx/mysql-schema.sql
 docker exec -i mysql8-nacos mysql -uroot -p nacos_config < mysql-schema.sql

docker pull nacos/nacos-server:v2.3.2
//no matching manifest for linux/arm64/v8 in the manifest list entries
这说明nacos/nacos-server:v2.3.2 官方镜像没有适配 ARM64 架构（你的 MacBook Air 是 Apple Silicon 芯片，属于 arm64 架构）。

解决方案：
docker pull --platform=linux/amd64 nacos/nacos-server:v2.3.2

启动的nacos连接不上mysql
解决方案如下：使用同一个网络  服务主机名设置为mysql容器名

docker run -d --name 1-nacos-1 --network nacos-net \
--platform=linux/amd64 \
-e MODE=standalone \
-e SPRING_DATASOURCE_PLATFORM=mysql \
-e MYSQL_SERVICE_HOST=mysql8-nacos \
-e MYSQL_SERVICE_PORT=3306 \
-e MYSQL_SERVICE_DB_NAME=nacos_config \
-e MYSQL_SERVICE_USER=root \
-e MYSQL_SERVICE_PASSWORD=admin \
-p 8848:8848 \
-p 9848:9848 \
nacos/nacos-server:v2.3.2


docker run -d \
--name nacos-standalone \
--platform=linux/amd64
-p 8848:8848 \
-p 9848:9848 \
-p 9849:9849 \
-e MODE=standalone \
-e PREFER_HOST_MODE=hostname \
nacos/nacos-server:v2.2.3

为什么需要引入bootstrap配置文件？
加载时机：它在application配置加载之前
用途：主要用于配置应用程序启动所需的一些“基础”或“外部化”的设置，特别是那些在应用程序上下文创建之前 就需要知道的配置。最典型的例子就是配置中心 的客户端配置