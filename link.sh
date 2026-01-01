#!/bin/bash

# 创建必要的目录
mkdir -p "build/libs"
mkdir -p ".gradle"

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
    # 创建核心代码的符号链接
    core_link="$dir/src/main/java/com/coloryr/allmusic/client/core"
    if [ ! -e "$core_link" ]; then
        mkdir -p "$(dirname "$core_link")"
        # 使用相对路径：从core_link所在目录到项目根目录的core
        (cd "$(dirname "$core_link")" && ln -sf "../../../../../../../../core" "$(basename "$core_link")")
    fi
    
    # 创建decoder目录
    decoder_dir="$dir/src/main/resources/com/coloryr/allmusic/client/core/player/decoder"
    if [ ! -d "$decoder_dir" ]; then
        mkdir -p "$decoder_dir"
    fi
    
    # 创建mp3资源的符号链接
    mp3_link="$decoder_dir/mp3"
    if [ ! -e "$mp3_link" ]; then
        # 使用相对路径：从mp3_link所在目录到项目根目录的mp3
        (cd "$(dirname "$mp3_link")" && ln -sf "../../../../../../../../../../mp3" "$(basename "$mp3_link")")
    fi
    
    # 创建build目录和libs符号链接
    if [ ! -d "$dir/build" ]; then
        mkdir -p "$dir/build"
        (cd "$dir/build" && ln -sf "../../build/libs" "libs")
    fi
done

echo "符号链接创建完成！"
