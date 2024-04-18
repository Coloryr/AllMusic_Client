@echo off

mkdir "build"
mkdir ".gradle"

setlocal enabledelayedexpansion

set array=fabric_1_14_4 fabric_1_52_2 fabric_1_16_5 fabric_1_17_1 fabric_1_18_2 ^
forge_1_19 fabric_1_19_3 fabric_1_19_4 fabric_1_20 ^
forge_1_7_10 forge_1_12_2 forge_1_14_4 forge_1_15_2 forge_1_16_5 ^
forge_1_17_1 forge_1_18_2 forge_1_19_2 forge_1_19_3 forge_1_20 ^
forge_1_20_2

for %%i in (%array%) do (
    if not exist "%%i\src\main\java\coloryr\allmusic_client" mkdir "%%i\src\main\java\coloryr\allmusic_client"
    if not exist "%%i\src\main\resources\coloryr\allmusic_client\player\decoder" mkdir "%%i\src\main\resources\coloryr\allmusic_client\player\decoder"

    :: 创建 Junction 点
    if not exist "%%i\src\main\resources\coloryr\allmusic_client\player\decoder\mp3" mklink /j "%%i\src\main\resources\coloryr\allmusic_client\player\decoder\mp3" "mp3"
    if not exist "%%i\src\main\java\coloryr\allmusic_client\player" mklink /j "%%i\src\main\java\coloryr\allmusic_client\player" "player"
    if not exist "%%i\src\main\java\coloryr\allmusic_client\hud" mklink /j "%%i\src\main\java\coloryr\allmusic_client\hud" "hud"
    if not exist "%%i\build" mklink /j "%%i\build" "build"
    if not exist "%%i\.gradle" mklink /j "%%i\.gradle" ".gradle"
)