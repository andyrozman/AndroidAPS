package info.nightscout.androidaps.plugins.pump.tandem.dialogs

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import android.widget.EditText
import androidx.core.app.ActivityCompat
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.event.EventPumpNeedsPairingCode
import info.nightscout.androidaps.plugins.pump.tandem.event.EventPumpPairingCodeProvided
import info.nightscout.core.ui.toast.ToastUtils

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import info.nightscout.pump.common.ui.PumpBLEConfigActivity
import info.nightscout.rx.AapsSchedulers
import info.nightscout.rx.logging.LTag
import javax.inject.Inject

class TandemPumpBLEConfigActivity : PumpBLEConfigActivity() {

    private val disposable = CompositeDisposable()
    @Inject lateinit var aapsSchedulers: AapsSchedulers

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
    }

    private fun askForPairingCode(e: EventPumpNeedsPairingCode) {
        aapsLogger.info(LTag.PUMPCOMM, "askForPairingCode")

        val builder = AlertDialog.Builder(this, info.nightscout.core.ui.R.style.AppTheme)
        builder.setTitle("Enter pairing code")
        builder.setMessage(e.instructions)

        // Set up the input
        val input = EditText(this)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL

        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            val pairingCode = input.text.toString()
            //Timber.i("pairing code inputted: %s", pairingCode)
            //triggerImmediatePair(peripheral, pairingCode, challenge)

            aapsLogger.info(LTag.PUMPCOMM, "PairingCode provided: ${pairingCode}")
            rxBus.send(EventPumpPairingCodeProvided(pairingCode))

        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()

        // val input = EditText(this.context)
        // // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        // input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        //
        // MaterialAlertDialogBuilder(context, R.style.DialogTheme)
        //     .setCustomTitle(AlertDialogHelper.buildCustomTitle(context, "Enter pairing code"))
        //     .setMessage(e.instructions)
        //     .setView(input)
        //     .setPositiveButton(context.getString(R.string.ok)) { dialog: DialogInterface, _: Int ->
        //
        //         dialog.dismiss()
        //         SystemClock.sleep(100)
        //         val pairingCode = input.text.toString()
        //         aapsLogger.info(LTag.PUMPCOMM, "PairingCode provided: ${pairingCode}")
        //         rxBus.send(EventPumpPairingCodeProvided(pairingCode))
        //     }
        //     .setNegativeButton(context.getString(R.string.cancel)) { dialog: DialogInterface, _: Int -> {
        //         dialog.dismiss()
        //         SystemClock.sleep(100)
        //     }}
        //     .show()
        //     .setCanceledOnTouchOutside(false)
    }


    override fun onResume() {
        super.onResume()
        bleSelector.onResume()

        disposable += rxBus
            .toObservable(EventPumpNeedsPairingCode::class.java)
            .observeOn(aapsSchedulers.io)
            .subscribe {
                Thread {
                    handler.post {
                        askForPairingCode(it)
                    }
                }.start()
            }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isEnabled != true) bluetoothAdapter?.enable()
            prepareForScanning()
            updateCurrentlySelectedBTDevice()
        } else {
            ToastUtils.errorToast(context, context.getString(info.nightscout.core.ui.R.string.need_connect_permission))
            finish()
        }

    }




}