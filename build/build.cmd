@echo off

copy ..\android\bin\aircast-jar.jar .\android\

adt ^
 -package ^
 -target ane ../example/libs/AirCast.ane extension.xml ^
 -swc ../swc/bin/aircast-lib.swc ^
 -platform Android-ARM -C android . ^
 -platform Android-x86 -C android . ^
 -platform iPhone-ARM -C ios . ^
 -platform default -C default . 

pause