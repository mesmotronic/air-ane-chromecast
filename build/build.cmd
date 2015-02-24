@echo off

adt ^
 -package ^
 -target ane ../example/libs/AirCast.ane extension.xml ^
 -swc ../actionscript/bin/aircast-lib.swc ^
 -platform iPhone-ARM -C ios . ^
 -platform default -C default . 

REM -platform Android-ARM -C android . ^
REM -platform Android-x86 -C android . ^
