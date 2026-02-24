@echo off
setlocal

set "BIN_DIR=%~dp0"
set "APP_HOME=%BIN_DIR%.."

java ^
  --module-path "%APP_HOME%\lib" ^
  --add-modules ALL-MODULE-PATH ^
  --module org.myjtools.openbbt.cli/org.myjtools.openbbt.cli.MainCommand ^
  %*