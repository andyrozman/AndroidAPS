PUMP-COMMON
------------

Intention of this module is to have as many parts of Pump Driver as possible done here (for
example most of BLE devices need to pair and they have similar handlers for this, which could be
made abstract and reusable).

common
-------

PumpPluginAbstract - abstract class for creating new pump driver


driver
------
Contains most of reusable classes. Your driver needs to implement PumpDriverConfiguration to be
able to use this classes.


driver.ble
----------
Classes needed to be implemented to use BLE pairing dialog (ui.PumpBLEConfigActivity)


driver.history
--------------
Classed needed to be implemented to use Pump History dialog (ui.PumpHistoryActivity)