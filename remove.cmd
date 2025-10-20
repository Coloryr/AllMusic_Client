@echo off
setlocal enabledelayedexpansion

:: 定义数组元素
set array=fabric_1_14_4 fabric_1_15_2 fabric_1_16_5 fabric_1_17_1 fabric_1_18_2 ^
fabric_1_19 fabric_1_19_3 fabric_1_19_4 fabric_1_20_1 fabric_1_20_6 fabric_1_21 ^
forge_1_7_10 forge_1_12_2 forge_1_14_4 forge_1_15_2 forge_1_16_5 ^
forge_1_17_1 forge_1_18_2 forge_1_19_2 forge_1_19_3 forge_1_20_1 ^
forge_1_20_4 forge_1_20_6 forge_1_21 neoforge_1_20_4 neoforge_1_20_6 neoforge_1_21 ^
neoforge_1_21_3 fabric_1_21_3 neoforge_1_21_5 fabric_1_21_5 fabric_1_21_6 neoforge_1_21_6 fabric_1_21_8

:: 遍历数组
for %%a in (%array%) do (
    if exist "%%a\src\main\resources\com\coloryr\allmusic\client\core\player\decoder\mp3" (
        rmdir "%%a\src\main\resources\com\coloryr\allmusic\client\core\player\decoder\mp3"
    )
    if exist "%%a\src\main\java\com\coloryr\allmusic\client\core" (
        rmdir "%%a\src\main\java\com\coloryr\allmusic\client\core"
    )
    if exist "%%a\build" rmdir /s /q "%%a\build" "build"
    if exist "%%a\.gradle" rmdir /s /q "%%a\.gradle" ".gradle"
    if exist "%%a\.idea" rmdir /s /q "%%a\.idea" ".idea"
)

endlocal