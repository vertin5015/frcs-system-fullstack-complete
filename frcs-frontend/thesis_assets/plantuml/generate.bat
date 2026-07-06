@echo off
REM 需 JDK，plantuml.jar 同目录；生成 PNG+SVG 到 out\
set JAR=%~dp0plantuml.jar
set OUT=%~dp0out
if not exist "%OUT%" mkdir "%OUT%"
for %%F in ("%~dp0*.puml") do (
  java -jar "%JAR%" -charset UTF-8 -tsvg -tpng -o "%OUT%" "%%F"
)
echo Done. See %OUT%
