# Tandem Slim X2

At the time of creation of project there were only versions of pump available that don't support
any kind of looping (Tandem is extending pump with bolus and hoping something more)

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
        TimeSinceResetResponse (?)                WIP-2 
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
    
    
    GetReservoir                         |        WIP-2
        InsulinStatusResponse            |        WIP-2
    GetBatteryLevel                      |        WIP-2
        CurrentBatteryV1Response (?)     |
        CurrentBatteryV2Response (?)     |
    
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

## ROADMAP

- Pairing (AAPS Configuration) - DONE-WAITING-FOR-TEST (1)
- Connect to pump: 
  - Establish communication (connect/disconnect) - WIP-2  [2]
  - Read Battery Level - WIP-2 [2]
  - Read Reservoir Level - WIP-2 [2]
  - Get Time - WIP-2 [2]

- Level 2 commands:
  - Get TBR [4]
  - Get Settings [3]
  - Implement checks based on settings [4]
  - Change Pump Context based on settings (Max Bolus, Max Basal) [4]

- Level 3 commands:
  - Get Basal Profile [5]
  - Get History [4]
  - Parse History: [3] 
    - TimeChangeHistoryLog [3]
    - DateChangeHistoryLog
    - BolusRequestedMsg1HistoryLog
    - BolusRequestedMsg2HistoryLog
    - BolusRequestedMsg3HistoryLog
    - BolusDeliveryHistoryLog
    - BolusCompletedHistoryLog
    - BolexCompletedHistoryLog
    - ...

- Level 4 commands:
  - Get TBR Status [6]
  - Get Bolus Status [6]
  - Get Pump Status [6]

- Level 5 commands:
  - Set Bolus [7]
  - Set TBR [8]
  - Set Profile [9]
  - Set Time [10]

- AAPS:
  - Open Loop mode  



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


## History reading
- configurable time to read history 5, 10, 15, 30 minutes
- reading history in AAPS
- decoding history (*should be done in pumpX2 project*)
- write history into database

***





