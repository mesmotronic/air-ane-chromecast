@echo off

adt ^
 -package ^
 -target ane ../example/libs/AirCast.ane extension.xml ^
 -swc ../swc/bin/aircast-lib.swc ^
 -platform iPhone-ARM -C ios . ^
 -platform default -C default . 

pause