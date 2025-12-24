@echo off

setlocal enabledelayedexpansion
:: 版本列表
set "PROJECTS[0]=forge_1_7_10"
set "PROJECTS[1]=forge_1_12_2"
set "PROJECTS[2]=forge_1_14_4"
set "PROJECTS[3]=forge_1_15_2"
set "PROJECTS[4]=forge_1_16_5"
set "PROJECTS[5]=forge_1_17_1"
set "PROJECTS[6]=forge_1_18_2"
set "PROJECTS[7]=forge_1_19_2"
set "PROJECTS[8]=forge_1_19_3"
set "PROJECTS[9]=forge_1_20_1"
set "PROJECTS[10]=forge_1_20_4"
set "PROJECTS[11]=forge_1_20_6"
set "PROJECTS[12]=forge_1_21"
set "PROJECTS[13]=fabric_1_14_4"
set "PROJECTS[14]=fabric_1_15_2"
set "PROJECTS[15]=fabric_1_16_5"
set "PROJECTS[16]=fabric_1_17_1"
set "PROJECTS[17]=fabric_1_18_2"
set "PROJECTS[18]=fabric_1_19"
set "PROJECTS[19]=fabric_1_19_3"
set "PROJECTS[20]=fabric_1_19_4"
set "PROJECTS[21]=fabric_1_20_1"
set "PROJECTS[22]=fabric_1_20_6"
set "PROJECTS[23]=fabric_1_21"
set "PROJECTS[24]=fabric_1_21_3"
set "PROJECTS[25]=fabric_1_21_5"
set "PROJECTS[26]=fabric_1_21_6"
set "PROJECTS[27]=fabric_26_1"
set "PROJECTS[28]=neoforge_1_20_4"
set "PROJECTS[29]=neoforge_1_20_6"
set "PROJECTS[30]=neoforge_1_21"
set "PROJECTS[31]=neoforge_1_21_3"
set "PROJECTS[32]=neoforge_1_21_5"
set "PROJECTS[33]=neoforge_1_21_6"

set /a ARRAY_LENGTH=33

cls

set "LINK_SCRIPT=link.cmd"
echo 正在执行预处理脚本: %LINK_SCRIPT%
call "%LINK_SCRIPT%" || (
    echo 预处理脚本失败，退出编译流程
    pause
    exit /b 1
)

:menu
cls
echo AllMusic客户端编译
echo 选择需要构建的版本：
echo ----------------------------

:: 生成菜单选项
for /L %%i in (0,1,%ARRAY_LENGTH%) do (
    call echo  [%%i] - %%PROJECTS[%%i]%%
)
echo ----------------------------
echo 请输入要编译的项目编号 (0-%ARRAY_LENGTH%):
set /p SELECTION=?

:: 验证输入是否为数字
if not defined SELECTION (
    echo 错误：未输入编号
    pause
    goto :menu
)
if not "%SELECTION%" == "" (
    set /a SELECTION=%SELECTION%
    if %SELECTION% LSS 0 (
        echo 错误：输入不能小于0
        pause
        goto :menu
    )
    if %SELECTION% GEQ %ARRAY_LENGTH%+1 (
        echo 错误：输入超过最大索引%ARRAY_LENGTH%
        pause
        goto :menu
    )
)

:: 获取选定路径并拆分盘符/路径
call set "SELECTED_PATH=%%PROJECTS[%SELECTION%]%%"

:: 检查路径存在性
if not exist "%SELECTED_PATH%" (
    echo 错误：路径不存在 - %SELECTED_PATH%
    pause
    goto :menu
)

:: 执行目录切换
cd /D "%SELECTED_PATH%"

:: 显示当前路径并执行编译
echo 当前工作目录：
cd
echo 正在执行Gradle编译...
call gradlew build

:: 编译结果处理
if %ERRORLEVEL% EQU 0 (
    echo 编译成功！生成位置：build\libs
) else (
    echo 编译失败，请检查错误信息
)
pause

goto :menu