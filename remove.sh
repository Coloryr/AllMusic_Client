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

# 遍历所有项目
for dir in "${projects[@]}"; do
    echo "处理项目: $dir"
    
    # 删除mp3符号链接
    mp3_link="$dir/src/main/resources/com/coloryr/allmusic/client/core/player/decoder/mp3"
    if [ -e "$mp3_link" ]; then
        echo "  删除mp3符号链接: $mp3_link"
        rm -f "$mp3_link"
    fi
    
    # 删除core符号链接
    core_link="$dir/src/main/java/com/coloryr/allmusic/client/core"
    if [ -e "$core_link" ]; then
        echo "  删除core符号链接: $core_link"
        rm -f "$core_link"
    fi
    
    # 删除build目录
    if [ -d "$dir/build" ]; then
        echo "  删除build目录: $dir/build"
        rm -rf "$dir/build"
    fi
    
    # 删除.gradle目录
    if [ -d "$dir/.gradle" ]; then
        echo "  删除.gradle目录: $dir/.gradle"
        rm -rf "$dir/.gradle"
    fi
    
    # 删除.idea目录
    if [ -d "$dir/.idea" ]; then
        echo "  删除.idea目录: $dir/.idea"
        rm -rf "$dir/.idea"
    fi
done

# 删除根目录的build和.gradle目录
if [ -d "build" ]; then
    echo "删除根目录build目录"
    rm -rf "build"
fi

if [ -d ".gradle" ]; then
    echo "删除根目录.gradle目录"
    rm -rf ".gradle"
fi

if [ -d ".idea" ]; then
    echo "删除根目录.idea目录"
    rm -rf ".idea"
fi

echo "清理完成！"
