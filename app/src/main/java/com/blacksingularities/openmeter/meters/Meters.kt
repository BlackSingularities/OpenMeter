package com.blacksingularities.openmeter.meters

import android.content.Context
import android.hardware.Sensor
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.blacksingularities.openmeter.R
import com.blacksingularities.openmeter.core.hasMicrophone
import com.blacksingularities.openmeter.core.hasSensor

/** Grupy mierników (filtry na ekranie głównym). */
enum class Category(@StringRes val label: Int) {
    Acoustics(R.string.cat_acoustics),
    Position(R.string.cat_position),
    Magnetism(R.string.cat_magnetism),
    Motion(R.string.cat_motion),
    Environment(R.string.cat_environment),
    Tools(R.string.cat_tools),
    Device(R.string.cat_device),
}

/** Wszystkie mierniki. Na ekranie głównym widać tylko te, dla których telefon ma czujnik. */
enum class Meter(
    val id: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val icon: ImageVector,
    val category: Category,
    /** Czy można zapisać bieżący odczyt do dziennika. */
    val snapshots: Boolean = true,
) {
    Sound("sound", R.string.m_sound, R.string.m_sound_sub, Icons.Rounded.GraphicEq, Category.Acoustics),
    Light("light", R.string.m_light, R.string.m_light_sub, Icons.Rounded.WbSunny, Category.Acoustics),
    Tone("tone", R.string.m_tone, R.string.m_tone_sub, Icons.Rounded.MusicNote, Category.Acoustics, snapshots = false),
    Level("level", R.string.m_level, R.string.m_level_sub, Icons.Rounded.Straighten, Category.Position),
    Protractor("protractor", R.string.m_protractor, R.string.m_protractor_sub, Icons.Rounded.Architecture, Category.Position),
    Metal("metal", R.string.m_metal, R.string.m_metal_sub, Icons.Rounded.Sensors, Category.Magnetism),
    Compass("compass", R.string.m_compass, R.string.m_compass_sub, Icons.Rounded.Explore, Category.Magnetism),
    Seismo("seismo", R.string.m_seismo, R.string.m_seismo_sub, Icons.Rounded.Waves, Category.Motion),
    Vibration("vibration", R.string.m_vibration, R.string.m_vibration_sub, Icons.Rounded.Train, Category.Motion),
    Tacho("tacho", R.string.m_tacho, R.string.m_tacho_sub, Icons.Rounded.Autorenew, Category.Motion),
    Steps("steps", R.string.m_steps, R.string.m_steps_sub, Icons.AutoMirrored.Rounded.DirectionsWalk, Category.Motion),
    Baro("baro", R.string.m_baro, R.string.m_baro_sub, Icons.Rounded.Speed, Category.Environment),
    Climate("climate", R.string.m_climate, R.string.m_climate_sub, Icons.Rounded.Thermostat, Category.Environment),
    Ruler("ruler", R.string.m_ruler, R.string.m_ruler_sub, Icons.Rounded.SquareFoot, Category.Tools),
    Battery("battery", R.string.m_battery, R.string.m_battery_sub, Icons.Rounded.BatteryChargingFull, Category.Device),
    Proximity("proximity", R.string.m_proximity, R.string.m_proximity_sub, Icons.Rounded.PanTool, Category.Device),
    Hinge("hinge", R.string.m_hinge, R.string.m_hinge_sub, Icons.AutoMirrored.Rounded.MenuBook, Category.Device),
    Inspector("inspector", R.string.m_inspector, R.string.m_inspector_sub, Icons.Rounded.Memory, Category.Device, snapshots = false),
    ;

    fun isAvailable(c: Context): Boolean = when (this) {
        Sound -> c.hasMicrophone()
        Light -> c.hasSensor(Sensor.TYPE_LIGHT)
        Tone, Ruler, Battery, Inspector -> true
        Level, Protractor, Seismo, Vibration -> c.hasSensor(Sensor.TYPE_ACCELEROMETER)
        Metal -> c.hasSensor(Sensor.TYPE_MAGNETIC_FIELD)
        Compass -> c.hasSensor(Sensor.TYPE_ROTATION_VECTOR) ||
            (c.hasSensor(Sensor.TYPE_MAGNETIC_FIELD) && c.hasSensor(Sensor.TYPE_ACCELEROMETER))
        Baro -> c.hasSensor(Sensor.TYPE_PRESSURE)
        Climate -> c.hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) || c.hasSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        Tacho -> c.hasSensor(Sensor.TYPE_GYROSCOPE)
        Steps -> c.hasSensor(Sensor.TYPE_STEP_DETECTOR) || c.hasSensor(Sensor.TYPE_STEP_COUNTER)
        Proximity -> c.hasSensor(Sensor.TYPE_PROXIMITY)
        Hinge -> Build.VERSION.SDK_INT >= 30 && c.hasSensor(Sensor.TYPE_HINGE_ANGLE)
    }

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

@Composable
fun MeterScreen(meter: Meter, onBack: () -> Unit) {
    when (meter) {
        Meter.Sound -> SoundMeter(onBack)
        Meter.Light -> LightMeter(onBack)
        Meter.Tone -> ToneGeneratorTool(onBack)
        Meter.Level -> LevelMeter(onBack)
        Meter.Protractor -> ProtractorMeter(onBack)
        Meter.Metal -> MetalDetector(onBack)
        Meter.Compass -> CompassMeter(onBack)
        Meter.Seismo -> Seismograph(onBack)
        Meter.Vibration -> VibrationMeter(onBack)
        Meter.Tacho -> Tachometer(onBack)
        Meter.Steps -> StepsMeter(onBack)
        Meter.Baro -> Barometer(onBack)
        Meter.Climate -> ClimateMeter(onBack)
        Meter.Ruler -> RulerTool(onBack)
        Meter.Battery -> BatteryMeter(onBack)
        Meter.Proximity -> ProximityMeter(onBack)
        Meter.Hinge -> HingeMeter(onBack)
        Meter.Inspector -> SensorInspector(onBack)
    }
}
