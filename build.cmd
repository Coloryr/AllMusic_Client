@echo off
setlocal enabledelayedexpansion

:: 定义数组元素
set list=fabric_1_14_4 fabric_1_52_2 fabric_1_16_5 fabric_1_17_1 fabric_1_18_2 ^
forge_1_19 fabric_1_19_3 fabric_1_19_4 fabric_1_20 ^
forge_1_7_10 forge_1_12_2 forge_1_14_4 forge_1_15_2 forge_1_16_5 ^
forge_1_17_1 forge_1_18_2 forge_1_19_2 forge_1_19_3 forge_1_20 ^
forge_1_20_2

:: 遍历数组
for %%a in (%list%) do (
    cd %%a
    gradlew build
    cd ..
)

endlocal