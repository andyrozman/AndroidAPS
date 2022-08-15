Tandem X2
-----------

At the time of creation of project there were only versions of pump available that don't support
any kind of looping (Tandem is extending pump with bolus and hoping something more)


History reading - NOT NEEDED configurable time to read history 5, 10, 15, 30 minutes


COMANDS:                             |       2.2        2.5
GetFirmware                          |       OK
    PumpVersionResponse              |       OK
GetHistory                           |
    ...
GetBasalProfile                      |
    ProfileStatusResponse (?)????
GetTime                              |
    TimeSinceResetResponse (?)
GetSettings                          |                                            See down...
    RemindersResponse,
    PumpSettingsResponse,
    PumpGlobalsResponse,
    GlobalMaxBolusSettingsResponse
    ControlIQInfoV1Response (?)
    ControlIQInfoV2Response (?)

GetBolus                             |        -
    CurrentBolusStatusResponse
    LastBolusStatusV2Response,
GetTBR
    TempRateResponse


GetReservoir                         |       WIP
    InsulinStatusResponse
GetBatteryLevel                      |       WIP
    CurrentBatteryV1Response (?)
    CurrentBatteryV2Response (?)

GetStatus (is pump running, bolus, tbr running)


SetBolus                             |        -
SetTBR                               |        -
SetBasalProfile                      |        -
SetTime                              |        -



DO EXISTS?
GetStatus

Settings:
RemindersResponse, PumpSettingsResponse, PumpGlobalsResponse, GlobalMaxBolusSettingsResponse

CHECKS ON START:
    - Control IQ NOT Running
    - Basal Profile Set to 1




- = Not available
1 = Implemented
2 = Present in driver
OK = Integrated
NI = Not implemented



Tandem Driver Implementation
=============================

At the moment Tandem doesn't support remote commands, but newest version of firmware will support
 at least some of them (remote bolus).

COMMANDS WISHLIST:

Command Type             Status             AAPS Implementation



