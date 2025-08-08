@echo off
echo ========================================
echo SwiftQ 全功能演示程序启动
echo SwiftQ Complete Feature Demo Starting
echo ========================================
echo.

cd /d "C:\Users\Administrator\Desktop\CodeBase\SwiftMQ"

echo 正在编译项目...
call mvn clean compile -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败，请检查代码
    pause
    exit /b 1
)

echo 编译成功，启动演示程序...
echo.

java -cp "swiftq-common/target/classes;swiftq-core/target/classes" com.swiftq.core.ComprehensiveDemo

echo.
echo ========================================
echo 演示程序执行完成
echo Demo Execution Completed
echo ========================================
pause
