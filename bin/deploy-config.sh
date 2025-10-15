#!/bin/bash

# ================================
# 配置推送脚本：将 global.properties 推送到 Nacos
# ================================

set -e  # 遇错退出

# === 1. 基础配置（请根据实际情况修改）===
NACOS_SERVER="http://10.50.2.172:8848"   # Nacos 地址
NAMESPACE="test"                            # 命名空间ID（留空表示 public）
GROUP="DEFAULT_GROUP"                   # 分组
DATA_ID="global.properties"             # Data ID（建议与文件名一致）
USERNAME="nacos"                             # Nacos 用户名（如开启鉴权）
PASSWORD="Fk@(a*(I1B"                             # Nacos 密码

# === 2. 检查依赖 ===
command -v curl >/dev/null 2>&1 || { echo "Error: curl is required."; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "Error: python3 is required for URL encoding."; exit 1; }

# === 3. 读取配置文件 ===
CONFIG_FILE="global.properties"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: $CONFIG_FILE not found!"
    exit 1
fi

echo "Reading config from $CONFIG_FILE..."

# 读取内容并去除空行和注释（可选）
# CONTENT=$(grep -v '^#' "$CONFIG_FILE" | grep -v '^$' | tr '\n' '\001' | sed 's/\001/\\n/g')
# 如果要保留注释和空行，直接读取：
CONTENT=$(cat "$CONFIG_FILE")

# === 4. URL 编码（关键！避免特殊字符如 = & % 导致解析错误）===
ENCODED_CONTENT=$(python3 -c "
import urllib.parse
import sys
content = sys.stdin.read()
print(urllib.parse.quote(content, safe=''))
" <<< "$CONTENT")

# === 5. 获取 Access Token（如果配置了用户名密码）===
ACCESS_TOKEN=""
if [ -n "$USERNAME" ] && [ -n "$PASSWORD" ]; then
    echo "Authenticating with Nacos..."
    TOKEN_RESPONSE=$(curl -s -X POST \
        "$NACOS_SERVER/nacos/v1/auth/users/login" \
        -d "username=$USERNAME&password=$PASSWORD")
    
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
    if [ -z "$ACCESS_TOKEN" ]; then
        echo "Error: Failed to get access token."
        exit 1
    fi
    AUTH_HEADER="-H Authorization: Bearer $ACCESS_TOKEN"
else
    AUTH_HEADER=""
fi

# === 6. 构建请求参数 ===
NAMESPACE_PARAM=""
if [ -n "$NAMESPACE" ]; then
    NAMESPACE_PARAM="&namespaceId=$NAMESPACE"
fi

# === 7. 推送配置到 Nacos ===
echo "Publishing config to Nacos..."
echo "  Server: $NACOS_SERVER"
echo "  Data ID: $DATA_ID"
echo "  Group: $GROUP"
[ -n "$NAMESPACE" ] && echo "  Namespace: $NAMESPACE"

# 直接使用用户名和密码作为参数
RESPONSE=$(curl -s -w "%{http_code}" -X POST \
    "$NACOS_SERVER/nacos/v1/cs/configs?dataId=$DATA_ID&group=$GROUP$NAMESPACE_PARAM" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "content=$ENCODED_CONTENT" \
    --data-urlencode "username=$USERNAME" \
    --data-urlencode "password=$PASSWORD")

HTTP_CODE="${RESPONSE: -3}"
BODY="${RESPONSE%???}"

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    if [[ "$BODY" == "true" ]]; then
        echo "✅ Success: Configuration published to Nacos."
    else
        echo "⚠️ Warning: Nacos returned HTTP $HTTP_CODE but body is not 'true'."
        echo "Response: $BODY"
    fi
else
    echo "❌ Error: Failed to publish config. HTTP $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi