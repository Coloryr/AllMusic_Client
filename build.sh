#!/bin/bash

# 定义项目数组
projects=(
    "fabric_1_16_5"
    "fabric_1_20_1"
    "fabric_1_21"
    "fabric_1_21_6"
    "fabric_1_21_11"
    "fabric_26_1"
    "forge_1_7_10"
    "forge_1_12_2"
    "forge_1_16_5"
    "forge_1_20_1"
    "neoforge_1_21"
    "neoforge_1_21_6"
    "neoforge_1_21_11"
)

# 执行预处理脚本
echo "正在执行预处理脚本: link.sh"
if ! ./link.sh; then
    echo "预处理脚本失败，退出编译流程"
    exit 1
fi

# 显示菜单函数
show_menu() {
    clear
    echo "AllMusic客户端编译"
    echo "选择需要构建的版本："
    echo "------------------------"
    
    # 显示所有项目选项
    for i in "${!projects[@]}"; do
        printf " [%2d] - %s\n" "$i" "${projects[$i]}"
    done
    
    echo "------------------------"
    echo "请输入要编译的项目编号 (0-$(( ${#projects[@]} - 1 ))):"
}

# 主循环
while true; do
    show_menu
    
    # 读取用户输入
    read -r selection
    
    # 验证输入
    if [[ -z "$selection" ]]; then
        echo "错误：未输入编号"
        read -p "按回车键继续..."
        continue
    fi
    
    # 检查是否为数字
    if ! [[ "$selection" =~ ^[0-9]+$ ]]; then
        echo "错误：请输入数字"
        read -p "按回车键继续..."
        continue
    fi
    
    # 检查范围
    if (( selection < 0 )) || (( selection >= ${#projects[@]} )); then
        echo "错误：输入超出范围 (0-$(( ${#projects[@]} - 1 )))"
        read -p "按回车键继续..."
        continue
    fi
    
    # 获取选定的项目路径
    selected_path="${projects[$selection]}"
    
    # 检查路径是否存在
    if [ ! -d "$selected_path" ]; then
        echo "错误：路径不存在 - $selected_path"
        read -p "按回车键继续..."
        continue
    fi
    
    # 切换到项目目录
    echo "切换到目录: $selected_path"
    cd "$selected_path" || {
        echo "错误：无法切换到目录 $selected_path"
        read -p "按回车键继续..."
        continue
    }
    
    # 显示当前目录
    echo "当前工作目录："
    pwd
    
    # 执行Gradle编译
    echo "正在执行Gradle编译..."
    if ./gradlew build; then
        echo "编译成功！生成位置：build/libs"
    else
        echo "编译失败，请检查错误信息"
    fi
    
    # 返回原始目录
    cd - > /dev/null
    
    echo ""
    read -p "按回车键返回菜单..."
done
