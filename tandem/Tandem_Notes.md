# Tandem X2

At the time of creation of project there were only versions of pump available that don't support
any kind of looping (Tandem is extending pump with bolus and hoping something more)

***

History reading - NOT NEEDED 
configurable time to read history 5, 10, 15, 30 minutes

***

## Commands

    COMANDS:                             |       2.2        2.5
    
    Pairing                                      WIP-2(*)
    
    GetFirmware                          |       OK-?
        PumpVersionResponse              |       OK-?
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
        ControlIQInfoV1Response (?)               WIP-2
        ControlIQInfoV2Response (?)               WIP-2
    
    GetBolus                             |        -
        CurrentBolusStatusResponse
        LastBolusStatusV2Response,
    GetTBR
        TempRateResponse
    
    
    GetReservoir                         |       WIP-2
        InsulinStatusResponse
    GetBatteryLevel                      |       WIP-2
        CurrentBatteryV1Response (?)
        CurrentBatteryV2Response (?)
    
    GetStatus (is pump running, bolus, tbr running)
        ???
        
    SetBolus                             |        -
    SetTBR                               |        -
    SetBasalProfile                      |        -
    SetTime                              |        -

    - = Not available
    1 = Implemented
    2 = Present in driver
    OK = Integrated
    NI = Not implemented
    WIP = Work in Progress
    WIP-2 = Work in Progress (implemented but not tested)

    * Pairing is implemented, but dialog will need to be exchanged 
      for more native one



***

### DO EXISTS?
GetStatus

***

### CHECKS ON START

 - Control IQ NOT Running
 - Basal Profile Set to 0

***

## How to help test

Project is not ready to be tested yet, but for some prelimiminary testing 
here are simple instructions.

1. Download jwoglom's pumpX2 repository
2. Build sources:  ./gradlew build
3. Deploy artifacts to local repository: ./gradlew publishToMavenLocal
4. Download this repository and build AAPS
5. Deploy to your Phone, recommended to use Android 11 
6. Follow instructions from developer on what to test. (You need at least Bolus-IQ enabled 
   version, It needs to have Tandem Pump API at version 2.2)




Tandem Driver Implementation
=============================

At the moment Tandem doesn't support remote commands, but newest version of firmware will support
 at least some of them (remote bolus).

COMMANDS WISHLIST:

Command Type             Status             AAPS Implementation



