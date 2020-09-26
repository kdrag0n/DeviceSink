package dev.kdrag0n.devicecontrols.demo.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.ControlAction
import android.service.controls.templates.StatelessTemplate
import androidx.annotation.RequiresApi
import dev.kdrag0n.devicecontrols.demo.MainActivity
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.reactivestreams.FlowAdapters
import timber.log.Timber
import java.util.*
import java.util.concurrent.Flow
import java.util.function.Consumer

private val allDeviceTypes = mapOf(
    Pair(DeviceTypes.TYPE_GENERIC_ON_OFF, "GENERIC_ON_OFF"),
    Pair(DeviceTypes.TYPE_GENERIC_START_STOP, "GENERIC_START_STOP"),
    Pair(DeviceTypes.TYPE_GENERIC_OPEN_CLOSE, "GENERIC_OPEN_CLOSE"),
    Pair(DeviceTypes.TYPE_GENERIC_LOCK_UNLOCK, "GENERIC_LOCK_UNLOCK"),
    Pair(DeviceTypes.TYPE_GENERIC_ARM_DISARM, "GENERIC_ARM_DISARM"),
    Pair(DeviceTypes.TYPE_GENERIC_TEMP_SETTING, "GENERIC_TEMP_SETTING"),
    Pair(DeviceTypes.TYPE_GENERIC_VIEWSTREAM, "GENERIC_VIEWSTREAM"),
    Pair(DeviceTypes.TYPE_UNKNOWN, "UNKNOWN"),
    Pair(DeviceTypes.TYPE_AC_HEATER, "AC_HEATER"),
    Pair(DeviceTypes.TYPE_AC_UNIT, "AC_UNIT"),
    Pair(DeviceTypes.TYPE_AIR_FRESHENER, "AIR_FRESHENER"),
    Pair(DeviceTypes.TYPE_AIR_PURIFIER, "AIR_PURIFIER"),
    Pair(DeviceTypes.TYPE_COFFEE_MAKER, "COFFEE_MAKER"),
    Pair(DeviceTypes.TYPE_DEHUMIDIFIER, "DEHUMIDIFIER"),
    Pair(DeviceTypes.TYPE_DISPLAY, "DISPLAY"),
    Pair(DeviceTypes.TYPE_FAN, "FAN"),
    Pair(DeviceTypes.TYPE_HOOD, "HOOD"),
    Pair(DeviceTypes.TYPE_HUMIDIFIER, "HUMIDIFIER"),
    Pair(DeviceTypes.TYPE_KETTLE, "KETTLE"),
    Pair(DeviceTypes.TYPE_LIGHT, "LIGHT"),
    Pair(DeviceTypes.TYPE_MICROWAVE, "MICROWAVE"),
    Pair(DeviceTypes.TYPE_OUTLET, "OUTLET"),
    Pair(DeviceTypes.TYPE_RADIATOR, "RADIATOR"),
    Pair(DeviceTypes.TYPE_REMOTE_CONTROL, "REMOTE_CONTROL"),
    Pair(DeviceTypes.TYPE_SET_TOP, "SET_TOP"),
    Pair(DeviceTypes.TYPE_STANDMIXER, "STANDMIXER"),
    Pair(DeviceTypes.TYPE_STYLER, "STYLER"),
    Pair(DeviceTypes.TYPE_SWITCH, "SWITCH"),
    Pair(DeviceTypes.TYPE_TV, "TV"),
    Pair(DeviceTypes.TYPE_WATER_HEATER, "WATER_HEATER"),
    Pair(DeviceTypes.TYPE_DISHWASHER, "DISHWASHER"),
    Pair(DeviceTypes.TYPE_DRYER, "DRYER"),
    Pair(DeviceTypes.TYPE_MOP, "MOP"),
    Pair(DeviceTypes.TYPE_MOWER, "MOWER"),
    Pair(DeviceTypes.TYPE_MULTICOOKER, "MULTICOOKER"),
    Pair(DeviceTypes.TYPE_SHOWER, "SHOWER"),
    Pair(DeviceTypes.TYPE_SPRINKLER, "SPRINKLER"),
    Pair(DeviceTypes.TYPE_WASHER, "WASHER"),
    Pair(DeviceTypes.TYPE_VACUUM, "VACUUM"),
    Pair(DeviceTypes.TYPE_AWNING, "AWNING"),
    Pair(DeviceTypes.TYPE_BLINDS, "BLINDS"),
    Pair(DeviceTypes.TYPE_CLOSET, "CLOSET"),
    Pair(DeviceTypes.TYPE_CURTAIN, "CURTAIN"),
    Pair(DeviceTypes.TYPE_DOOR, "DOOR"),
    Pair(DeviceTypes.TYPE_DRAWER, "DRAWER"),
    Pair(DeviceTypes.TYPE_GARAGE, "GARAGE"),
    Pair(DeviceTypes.TYPE_GATE, "GATE"),
    Pair(DeviceTypes.TYPE_PERGOLA, "PERGOLA"),
    Pair(DeviceTypes.TYPE_SHUTTER, "SHUTTER"),
    Pair(DeviceTypes.TYPE_WINDOW, "WINDOW"),
    Pair(DeviceTypes.TYPE_VALVE, "VALVE"),
    Pair(DeviceTypes.TYPE_LOCK, "LOCK"),
    Pair(DeviceTypes.TYPE_SECURITY_SYSTEM, "SECURITY_SYSTEM"),
    Pair(DeviceTypes.TYPE_HEATER, "HEATER"),
    Pair(DeviceTypes.TYPE_REFRIGERATOR, "REFRIGERATOR"),
    Pair(DeviceTypes.TYPE_THERMOSTAT, "THERMOSTAT"),
    Pair(DeviceTypes.TYPE_CAMERA, "CAMERA"),
    Pair(DeviceTypes.TYPE_DOORBELL, "DOORBELL"),
    Pair(DeviceTypes.TYPE_ROUTINE, "ROUTINE"),
)

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {
    private lateinit var updatePublisher: ReplayProcessor<Control>

    private val pi by lazy {
        PendingIntent.getActivity(
            baseContext, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val controls by lazy {
        createControls()
    }

    private fun createControls(): List<Control> =
        allDeviceTypes.map {
            val type = it.key
            val name = it.value

            val friendlyName = name.replace('_', ' ')
                .toLowerCase(Locale.getDefault())
                .capitalize(Locale.getDefault())

            Control.StatefulBuilder(name, pi)
                .setTitle(friendlyName)
                .setDeviceType(type)
                .setStatus(Control.STATUS_OK)
                .setControlTemplate(StatelessTemplate(name))
                .build()
        }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        Timber.i("create all pubs")
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls))
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        Timber.i("create pub for $controlIds")
        updatePublisher = ReplayProcessor.create()

        for ((idx, name) in allDeviceTypes.values.withIndex()) {
            if (name in controlIds) {
                updatePublisher.onNext(controls[idx])
            }
        }

        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        consumer.accept(ControlAction.RESPONSE_OK)
    }
}