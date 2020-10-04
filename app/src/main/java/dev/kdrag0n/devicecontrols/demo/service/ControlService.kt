package dev.kdrag0n.devicecontrols.demo.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.*
import android.service.controls.templates.*
import androidx.annotation.RequiresApi
import dev.kdrag0n.devicecontrols.demo.MainActivity
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.reactivestreams.FlowAdapters
import timber.log.Timber
import java.util.*
import java.util.concurrent.Flow
import java.util.function.Consumer
import kotlin.reflect.KClass

private val allDeviceTypes = listOf(
    Triple(DeviceTypes.TYPE_GENERIC_ON_OFF, "GENERIC_ON_OFF", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_START_STOP, "GENERIC_START_STOP", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_OPEN_CLOSE, "GENERIC_OPEN_CLOSE", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_LOCK_UNLOCK, "GENERIC_LOCK_UNLOCK", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_ARM_DISARM, "GENERIC_ARM_DISARM", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_TEMP_SETTING, "GENERIC_TEMP_SETTING", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_GENERIC_VIEWSTREAM, "GENERIC_VIEWSTREAM", StatelessTemplate::class), // TODO
    Triple(DeviceTypes.TYPE_UNKNOWN, "UNKNOWN", ControlTemplate::class),
    Triple(DeviceTypes.TYPE_AC_HEATER, "AC_HEATER", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_AC_UNIT, "AC_UNIT", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_AIR_FRESHENER, "AIR_FRESHENER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_AIR_PURIFIER, "AIR_PURIFIER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_COFFEE_MAKER, "COFFEE_MAKER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_DEHUMIDIFIER, "DEHUMIDIFIER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_DISPLAY, "DISPLAY", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_FAN, "FAN", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_HOOD, "HOOD", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_HUMIDIFIER, "HUMIDIFIER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_KETTLE, "KETTLE", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_LIGHT, "LIGHT", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_MICROWAVE, "MICROWAVE", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_OUTLET, "OUTLET", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_RADIATOR, "RADIATOR", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_REMOTE_CONTROL, "REMOTE_CONTROL", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_SET_TOP, "SET_TOP", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_STANDMIXER, "STANDMIXER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_STYLER, "STYLER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_SWITCH, "SWITCH", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_TV, "TV", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_WATER_HEATER, "WATER_HEATER", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_DISHWASHER, "DISHWASHER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_DRYER, "DRYER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_MOP, "MOP", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_MOWER, "MOWER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_MULTICOOKER, "MULTICOOKER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_SHOWER, "SHOWER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_SPRINKLER, "SPRINKLER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_WASHER, "WASHER", StatelessTemplate::class),
    Triple(DeviceTypes.TYPE_VACUUM, "VACUUM", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_AWNING, "AWNING", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_BLINDS, "BLINDS", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_CLOSET, "CLOSET", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_CURTAIN, "CURTAIN", ToggleRangeTemplate::class),
    Triple(DeviceTypes.TYPE_DOOR, "DOOR", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_DRAWER, "DRAWER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GARAGE, "GARAGE", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_GATE, "GATE", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_PERGOLA, "PERGOLA", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_SHUTTER, "SHUTTER", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_WINDOW, "WINDOW", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_VALVE, "VALVE", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_LOCK, "LOCK", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_SECURITY_SYSTEM, "SECURITY_SYSTEM", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_HEATER, "HEATER", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_REFRIGERATOR, "REFRIGERATOR", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_THERMOSTAT, "THERMOSTAT", TemperatureControlTemplate::class),
    Triple(DeviceTypes.TYPE_CAMERA, "CAMERA", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_DOORBELL, "DOORBELL", ToggleTemplate::class),
    Triple(DeviceTypes.TYPE_ROUTINE, "ROUTINE", StatelessTemplate::class),
)

private val nameOverrides = mapOf(
    Pair("AC_HEATER", "AC heater"),
    Pair("AC_UNIT", "AC unit"),
    Pair("TV", "TV"),
)

private val templateNames = mapOf(
    Pair(ControlTemplate::class, "Control"),
    Pair(StatelessTemplate::class, "Stateless"),
    Pair(ToggleTemplate::class, "Toggle"),
    Pair(ToggleRangeTemplate::class, "Toggle + range"),
    Pair(RangeTemplate::class, "Range"),
    Pair(TemperatureControlTemplate::class, "Temperature"),
)

private const val TEMPERATURE_FLAG_ALL_MODES = TemperatureControlTemplate.FLAG_MODE_OFF or
        TemperatureControlTemplate.FLAG_MODE_HEAT or
        TemperatureControlTemplate.FLAG_MODE_COOL or
        TemperatureControlTemplate.FLAG_MODE_HEAT_COOL or
        TemperatureControlTemplate.FLAG_MODE_ECO

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {
    private lateinit var updatePublisher: ReplayProcessor<Control>

    private val pi by lazy {
        PendingIntent.getActivity(
            baseContext,0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val controls by lazy {
        createControls()
    }

    private fun createControl(
        type: Int,
        name: String,
        templateType: KClass<out ControlTemplate>,
        active: Boolean = true,
        rangeValue: Float = 75f,
        tempMode: Int = TemperatureControlTemplate.MODE_HEAT,
    ): Control {
        val friendlyName =
            nameOverrides.getOrElse(name) {
                name.replace('_', ' ')
                    .toLowerCase(Locale.getDefault())
                    .capitalize(Locale.getDefault())
            }

        val (template, status) = when (templateType) {
            ControlTemplate::class -> Pair(ControlTemplate.getNoTemplateObject(), "")
            StatelessTemplate::class -> Pair(StatelessTemplate(name), "Tap to start")
            ToggleTemplate::class -> Pair(
                ToggleTemplate(name, ControlButton(active, "Toggle")),
                if (active) "On" else "Off",
            )
            RangeTemplate::class -> Pair(
                RangeTemplate(name, 1f, 100f, rangeValue, 1f, "%.0f"),
                "",
            )
            ToggleRangeTemplate::class -> Pair(
                ToggleRangeTemplate(name, active, "Toggle",
                    RangeTemplate(name, 1f, 100f, rangeValue, 1f, "• %.0f%%")
                ),
                if (active) "On" else "Off",
            )
            TemperatureControlTemplate::class -> Pair(
                TemperatureControlTemplate(name,
                    ToggleRangeTemplate(name, active, "Toggle",
                        RangeTemplate(name, 1f, 100f, rangeValue, 1f, "• %.0f°C")
                    ),
                    tempMode, tempMode,
                    TEMPERATURE_FLAG_ALL_MODES
                ),
                if (active) "On" else "Off",
            )
            else -> throw IllegalArgumentException("Unsupported template type $templateType")
        }

        val templateName = templateNames[templateType] ?: error("No name for template type")

        return Control.StatefulBuilder(name, pi)
            .setTitle(friendlyName)
            .setDeviceType(type)
            .setStatus(Control.STATUS_OK)
            .setStatusText(status)
            .setControlTemplate(template)
            .setSubtitle(templateName)
            .setZone(templateName)
            .build()
    }

    private fun createControls(): MutableMap<String, Control> {
        return allDeviceTypes.map {
            val (type, name, templateType) = it
            createControl(type, name, templateType)
        }.associateBy { it.controlId }.toMutableMap()
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        Timber.i("create all pubs")
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls.values))
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        Timber.i("create pub for $controlIds")
        updatePublisher = ReplayProcessor.create()

        for (controlId in controlIds) {
            updatePublisher.onNext(controls[controlId])
        }

        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        consumer.accept(ControlAction.RESPONSE_OK)
        val control = controls[controlId]
        val (type, name, templateType) = allDeviceTypes.first {
            it.second == controlId
        }

        val newControl = when (action::class) {
            BooleanAction::class -> createControl(type, name, templateType, active = (action as BooleanAction).newState)
            FloatAction::class -> createControl(type, name, templateType, rangeValue = (action as FloatAction).newValue)
            ModeAction::class -> createControl(type, name, templateType, tempMode = (action as ModeAction).newMode)
            else -> control
        }

        controls[controlId] = newControl as Control
        updatePublisher.onNext(newControl)
    }
}