# Tandem Slim X2

At the time of creation of project there were only versions of pump available that don't support
any kind of looping (Tandem is extending pump with bolus and hoping something more)

### Tandem Slim X2
For successful looping pump must support at least 2 commands: setting TBR and setting Bolus. At
the moment (according to official sources), Slim X2 will support only one of them, which will
make Closed-Loop impossible. So if you have Slim X2, you will be able to Open Loop only (unless
they add support for TBR too), but you be able to receive all information from the pump and AAPS
will give you pointers what to do.

Most of work that will be done here, will be precursor for next device (t:Sport or t:Mobi), which
 will support everything.



CHANGE LOG
----------
tandem-0.2.6-SNAPSHOT
0.2.6 - updated to 3.2.
0.2.5 - moved and renamed packages
0.2.4 - Merging with dev-g (9.2.2023)
0.2.3 - Pairing sort of works, start of history parsing


***


## Commands

    COMANDS:                             |       2.2        2.5
    
    Pairing                                      WIP-2(*)
    
    GetFirmware                          |       OK-?
        PumpVersionResponse              |       OK-?
    GetHistory                           |
        ...
    GetBasalProfile                      |       WIP-2
        ProfileStatusResponse +          |       WIP-2
        IDPSettingsResponse +            |       WIP-2
        IDPSegmentResponse               |       WIP-2
    GetTime                              |       WIP-2
        TimeSinceResetResponse           |       WIP-2
    GetSettings                          |       WIP-2
        RemindersResponse,               |       NN
        PumpSettingsResponse,            |       NN
        PumpGlobalsResponse,             |       NN
        GlobalMaxBolusSettingsResponse   |       WIP-2
        ControlIQInfoV1Response (?)      |       WIP-2
        ControlIQInfoV2Response (?)      |       WIP-2
    
    GetBolus                             |        ?
        CurrentBolusStatusResponse
        LastBolusStatusV2Response,
    GetTBR                               |       WIP-2
        TempRateResponse                 |       WIP-2
    
    
    GetReservoir                         |       WIP-2
        InsulinStatusResponse            |       WIP-2
    GetBatteryLevel                      |       WIP-2
        CurrentBatteryV1Response (?)     |       WIP-2
        CurrentBatteryV2Response (?)     |       WIP-2
    
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
    NN = Not needed

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

HIstory Support
----------------






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

Prepare X2 Library
1. Download jwoglom's pumpX2 repository (go into dev branch) - https://github.com/jwoglom/pumpX2
2. Build sources:  ./gradlew build
3. Deploy artifacts to local repository: ./gradlew publishToMavenLocal

Prepare Test Phone (if you don't have one)
1. Get a Phone (Android needs to be higher than 9, recommended that version is also lower than 12)
2. If you have done Objectives before import those settings, if you have Live AAPS you can use
   that. Change Nightscout URL first (we don't want test setup to send data to NS), then do
   export, and change URL back
3. Import old setting

Prepare APS
1. Download this repository (put it into separate folder than your running/live version) in this
   branch and build AAPS (name it something like AndroidAPS_Tandem) -
   https://github.com/andyrozman/AndroidAPS.git  (you neet to checkout andy_tandem branch)
2. Deploy to your Phone, recommended to use Android < 12
3. Follow instructions from developer on what to test. (You need at least Bolus-IQ enabled
   version, It needs to have Tandem Pump API at version 2.2)
4. Easiest way to deploy, is to connect your phone to your computer and run it from Android Studio,
   you will be able to see what is happening if you look into Logcat tab (logs)




## History reading
- configurable time to read history 5, 10, 15, 30 minutes
- reading history in AAPS
- decoding history (*should be done in pumpX2 project*)
- write history into database

***





