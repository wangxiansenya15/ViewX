#!/bin/bash

# ========================================
# ViewX 配置快速更新脚本
# ========================================
# 用途：在生产环境快速更新配置并重启服务
# 使用：./update-config.sh <配置项> <新值>

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查管理员Token
check_token() {
    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "请设置 ADMIN_TOKEN 环境变量"
        echo "示例: export ADMIN_TOKEN='your-admin-token'"
        exit 1
    fi
}

# 更新单个配置
update_config() {
    local key=$1
    local value=$2
    
    print_info "正在更新配置: $key"
    
    response=$(curl -s -X PUT "$API_BASE_URL/admin/config/$key" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"value\": \"$value\"}")
    
    if echo "$response" | grep -q '"code":200'; then
        print_info "配置更新成功"
        return 0
    else
        print_error "配置更新失败: $response"
        return 1
    fi
}

# 更新邮箱授权码（快捷方式）
update_mail_password() {
    local password=$1
    
    print_info "正在更新邮箱授权码..."
    
    response=$(curl -s -X PUT "$API_BASE_URL/admin/config/mail-password" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"password\": \"$password\"}")
    
    if echo "$response" | grep -q '"code":200'; then
        print_info "邮箱授权码更新成功"
        return 0
    else
        print_error "邮箱授权码更新失败: $response"
        return 1
    fi
}

# 查看所有配置
view_configs() {
    print_info "正在获取所有配置..."
    
    response=$(curl -s -X GET "$API_BASE_URL/admin/config" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# 备份配置
backup_config() {
    print_info "正在备份配置..."
    
    response=$(curl -s -X POST "$API_BASE_URL/admin/config/backup" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    
    if echo "$response" | grep -q '"code":200'; then
        print_info "配置备份成功"
        return 0
    else
        print_error "配置备份失败: $response"
        return 1
    fi
}

# 重启服务（仅支持 Maven）
restart_service() {
    print_warn "准备使用 Maven 重启服务..."
    
    # 检查是否有 mvn 命令
    if ! command -v mvn &> /dev/null; then
        print_error "未找到 Maven 命令，请先安装 Maven"
        exit 1
    fi
    
    # 查找项目根目录（包含 pom.xml 的目录）
    PROJECT_ROOT=$(pwd)
    while [ "$PROJECT_ROOT" != "/" ]; do
        if [ -f "$PROJECT_ROOT/pom.xml" ]; then
            break
        fi
        PROJECT_ROOT=$(dirname "$PROJECT_ROOT")
    done
    
    if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        print_error "未找到 pom.xml，请在项目目录下运行此脚本"
        exit 1
    fi
    
    print_info "项目根目录: $PROJECT_ROOT"
    cd "$PROJECT_ROOT" || exit 1
    
    # 停止现有进程
    print_info "正在停止现有服务..."
    pkill -f "spring-boot:run" || true
    sleep 2
    
    # 使用 Maven 启动
    print_info "正在使用 Maven 启动服务..."
    nohup mvn spring-boot:run > logs/app.log 2>&1 &
    
    print_info "服务已在后台启动，日志文件: logs/app.log"
    print_info "可使用 'tail -f logs/app.log' 查看日志"
}


# 显示帮助
show_help() {
    cat << EOF
ViewX 配置快速更新脚本

用法:
  $0 <命令> [参数]

命令:
  update <KEY> <VALUE>    更新指定配置项
  mail <PASSWORD>         快速更新邮箱授权码
  view                    查看所有配置
  backup                  备份当前配置
  restart                 重启服务

环境变量:
  ADMIN_TOKEN            管理员Token（必需）
  API_BASE_URL           API地址（默认: http://localhost:8080/api）

示例:
  # 设置Token
  export ADMIN_TOKEN='your-admin-token-here'
  
  # 更新邮箱授权码
  $0 mail 'new-password-123'
  
  # 更新AI密钥
  $0 update AI_API_KEY 'sk-new-key-123'
  
  # 查看所有配置
  $0 view
  
  # 备份配置
  $0 backup
  
  # 重启服务
  $0 restart

完整流程:
  1. 备份配置:   $0 backup
  2. 更新配置:   $0 mail 'new-password'
  3. 重启服务:   $0 restart

EOF
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    command=$1
    shift
    
    case $command in
        update)
            check_token
            if [ $# -ne 2 ]; then
                print_error "用法: $0 update <KEY> <VALUE>"
                exit 1
            fi
            update_config "$1" "$2"
            print_warn "配置已更新，请运行 '$0 restart' 重启服务使配置生效"
            ;;
        mail)
            check_token
            if [ $# -ne 1 ]; then
                print_error "用法: $0 mail <PASSWORD>"
                exit 1
            fi
            update_mail_password "$1"
            print_warn "配置已更新，请运行 '$0 restart' 重启服务使配置生效"
            ;;
        view)
            check_token
            view_configs
            ;;
        backup)
            check_token
            backup_config
            ;;
        restart)
            restart_service
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
