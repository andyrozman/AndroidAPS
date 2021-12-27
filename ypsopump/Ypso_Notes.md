YPSOPUMP
--------

At the time of creation of project there ware only versions of pump available that don't support looping (one
side communication only - we can read from pump but not send data - TBRs, Boluses or BasalProfile).


History reading - NOT NEEDED configurable time to read history 5, 10, 15, 30 minutes


COMANDS:                             |       1.5        1.6
GetFirmware                          |       OK         NI2
GetHistory (Events, Alarms)          |       3          NI2
GetBasalProfile                      |       2          NI2
GetTime                              |       OK         NI2
GetSettings                          |       OK         NI2

SetBolus                             |       -          NI2
SetTBR                               |       -          NI2
SetBasalProfile                      |       -          NI2
SetTime                              |       -          NI2


DO EXISTS?
GetStatus
GetReservoir
GetBatteryLevel




- = Not available
1 = Implemented
2 = Present in driver
OK = Integrated
NI = Not implemented


=============================================

What needs to be implemented:
- device detection in configuration
- pump changed PumpPlugin
- GetProfile and profile handling
- history: - get start/stop TBR
           - saving to Db
           - history dialog


=============================================

Instructions:

- Use Basal Profile A
- Configurable reading of History (time frame)


=============================================

Open Loop Implementation
------------------------
- Force Open Loop
- Disable SMB

- Set Time
- Set Profile
- Set Bolus
- Set TBR





