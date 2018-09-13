@echo off

cls

adt ^
 -package ^
 -target ane ../example/libs/AirCast.ane extension.xml ^
 -swc ../swc/bin/air-ane-aircast-swc.swc ^
 -platform iPhone-ARM -C iPhone-ARM . ^
 -platform default -C default . 

pause
