@echo off

mkdir "build"
mkdir ".gradle"

setlocal enabledelayedexpansion

set array=fabric_1_14_4 fabric_1_15_2 fabric_1_16_5 fabric_1_17_1 fabric_1_18_2 ^
fabric_1_19 fabric_1_19_3 fabric_1_19_4 fabric_1_20 fabric_1_20_6 fabric_1_21 ^
forge_1_7_10 forge_1_12_2 forge_1_14_4 forge_1_15_2 forge_1_16_5 ^
forge_1_17_1 forge_1_18_2 forge_1_19_2 forge_1_19_3 forge_1_20 ^
forge_1_20_4 forge_1_20_6 forge_1_21 neoforge_1_20_4 neoforge_1_20_6 neoforge_1_21 ^
neoforge_1_21_3 fabric_1_21_3

for %%i in (%array%) do (
    if not exist "%%i\src\main\java\com\coloryr\allmusic\client" mkdir "%%i\src\main\java\com\coloryr\allmusic\client"
    if not exist "%%i\src\main\resources\com\coloryr\allmusic\client\player\decoder" mkdir "%%i\src\main\resources\com\coloryr\allmusic\client\player\decoder"

    :: 创建 Junction 点
    if not exist "%%i\src\main\resources\com\coloryr\allmusic\client\player\decoder\mp3" mklink /j "%%i\src\main\resources\com\coloryr\allmusic\client\player\decoder\mp3" "mp3"
    if not exist "%%i\src\main\java\com\coloryr\allmusic\client\player" mklink /j "%%i\src\main\java\com\coloryr\allmusic\client\player" "player"
    if not exist "%%i\src\main\java\com\coloryr\allmusic\client\hud" mklink /j "%%i\src\main\java\com\coloryr\allmusic\client\hud" "hud"
    if not exist "%%i\build" mklink /j "%%i\build" "build"
    if not exist "%%i\.gradle" mklink /j "%%i\.gradle" ".gradle"
)

endlocal