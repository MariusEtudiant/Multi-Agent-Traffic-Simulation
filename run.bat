@echo off
set JAVA_FX=lib/javafx-sdk-24.0.1/lib
set TWEETY_LIB=lib/tweety
set CP=out;%TWEETY_LIB%\*;.

:: Compilation
echo Compilation...
for /R src %%f in (*.java) do echo %%f >> sources.txt

javac --module-path "%JAVA_FX%" --add-modules javafx.controls -cp "%TWEETY_LIB%\*" -d out @sources.txt

:: Exécution
echo Lancement...
java --module-path "%JAVA_FX%" --add-modules javafx.controls -cp "%CP%" org.example.gui.TrafficSimulatorApp

:: Nettoyage
del sources.txt
pause
