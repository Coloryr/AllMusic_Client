@echo off

mkdir "build\libs"
mkdir ".gradle"

setlocal enabledelayedexpansion

set array=fabric_1_16_5 fabric_1_20_1 fabric_1_21 fabric_1_21_6 ^
fabric_26_1 ^
forge_1_7_10 forge_1_12_2 forge_1_16_5 forge_1_20_1 ^
neoforge_1_21 neoforge_1_21_6

for %%i in (%array%) do (
    if not exist "%%i\src\main\java\com\coloryr\allmusic\client\core" mklink /j "%%i\src\main\java\com\coloryr\allmusic\client\core" "core"
    if not exist "%%i\src\main\resources\com\coloryr\allmusic\client\core\player\decoder" mkdir "%%i\src\main\resources\com\coloryr\allmusic\client\core\player\decoder"
    if not exist "%%i\src\main\resources\com\coloryr\allmusic\client\core\player\decoder\mp3" mklink /j "%%i\src\main\resources\com\coloryr\allmusic\client\core\player\decoder\mp3" "mp3"
    if not exist "%%i\build" mkdir "%%i\build" && mklink /j "%%i\build\libs" "build\libs"
)

endlocal