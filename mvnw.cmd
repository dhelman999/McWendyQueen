@ECHO OFF
SETLOCAL

SET "MVNW_VERBOSE="
IF NOT "%MVNW_VERBOSE%"=="" (
  SET "_MVNW_VERBOSE=1"
)

SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
SET "WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties"
SET "MVNW_DIR=%~dp0"
IF "%MVNW_DIR:~-1%"=="\" SET "MVNW_DIR=%MVNW_DIR:~0,-1%"

IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO Error: "%WRAPPER_PROPERTIES%" not found.
  EXIT /B 1
)

FOR /F "tokens=1,* delims==" %%A IN ('findstr /B /C:"wrapperUrl=" "%WRAPPER_PROPERTIES%"') DO (
  SET "WRAPPER_URL=%%B"
)

IF "%WRAPPER_URL%"=="" (
  SET "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
)

IF NOT EXIST "%WRAPPER_DIR%" (
  mkdir "%WRAPPER_DIR%"
)

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Downloading Maven wrapper jar from %WRAPPER_URL%
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  IF ERRORLEVEL 1 (
    ECHO Error: Failed to download Maven wrapper jar.
    EXIT /B 1
  )
)

SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
IF NOT EXIST "%JAVA_EXE%" (
  SET "JAVA_EXE=java.exe"
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MVNW_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
SET "MVNW_EXIT_CODE=%ERRORLEVEL%"

ENDLOCAL & EXIT /B %MVNW_EXIT_CODE%
