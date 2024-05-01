@echo off
setlocal enabledelayedexpansion

:: 定义数组元素
set array=fabric_1_14_4 fabric_1_15_2 fabric_1_16_5 fabric_1_17_1 fabric_1_18_2 ^
fabric_1_19 fabric_1_19_3 fabric_1_19_4 fabric_1_20 fabric_1_20_6 ^
forge_1_7_10 forge_1_12_2 forge_1_14_4 forge_1_15_2 forge_1_16_5 ^
forge_1_17_1 forge_1_18_2 forge_1_19_2 forge_1_19_3 forge_1_20 ^
forge_1_20_4 neoforge_1_20_4 neoforge_1_20_5

:: 遍历数组
for %%a in (%array%) do (
    if exist "%%a\src\main\resources\com\coloryr\allmusic\client\player\decoder\mp3" (
        rmdir "%%a\src\main\resources\com\coloryr\allmusic\client\player\decoder\mp3"
    )
    if exist "%%a\src\main\java\com\coloryr\allmusic\client\player" (
        rmdir "%%a\src\main\java\com\coloryr\allmusic\client\player"
    )
    if exist "%%a\src\main\java\com\coloryr\allmusic\client\hud" (
        rmdir "%%a\src\main\java\com\coloryr\allmusic\client\hud"
    )
    if exist "%%a\build" (
        rmdir /s /q "%%a\build"
    )
    if exist "%%a\.gradle" (
        rmdir /s /q "%%a\.gradle"
    )
)

endlocal