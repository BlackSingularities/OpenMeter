package com.blacksingularities.openmeter.meters

import android.content.Context
import android.hardware.Sensor
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.blacksingularities.openmeter.core.hasMicrophone
import com.blacksingularities.openmeter.core.hasSensor

/** Wszystkie mierniki. Na ekranie głównym widać tylko te, dla których telefon ma czujnik. */
enum class Meter(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Sound("sound", "Decybelomierz", "Poziom dźwięku, widmo, stroik", Icons.Rounded.GraphicEq),
    Light("light", "Luksomierz", "Natężenie światła i ekspozycja", Icons.Rounded.WbSunny),
    Level("level", "Poziomica", "Libella 2D i pozioma", Icons.Rounded.Straighten),
    Protractor("protractor", "Kątomierz", "Kąt krawędzi i nachylenie", Icons.Rounded.Architecture),
    Metal("metal", "Wykrywacz metalu", "Pole magnetyczne w µT", Icons.Rounded.Sensors),
    Compass("compass", "Kompas", "Azymut i pole magnetyczne", Icons.Rounded.Explore),
    Seismo("seismo", "Sejsmograf", "Drgania podłoża z akcelerometru", Icons.Rounded.Waves),
    Baro("baro", "Barometr", "Ciśnienie, wysokość, trend", Icons.Rounded.Speed),
    Vibration("vibration", "Drgania w podróży", "Komfort jazdy autem i pociągiem", Icons.Rounded.Train),
    Climate("climate", "Klimat", "Temperatura, wilgotność, punkt rosy", Icons.Rounded.Thermostat),
    Tacho("tacho", "Obrotomierz", "Prędkość obrotu z żyroskopu", Icons.Rounded.Autorenew),
    Steps("steps", "Krokomierz", "Kroki i kadencja", Icons.AutoMirrored.Rounded.DirectionsWalk),
    Proximity("proximity", "Zbliżenie", "Czujnik zbliżeniowy i licznik gestów", Icons.Rounded.PanTool),
    Hinge("hinge", "Kąt zawiasu", "Rozłożenie składanego telefonu", Icons.AutoMirrored.Rounded.MenuBook),
    Inspector("inspector", "Wszystkie czujniki", "Lista i odczyty na żywo", Icons.Rounded.Memory),
    ;

    fun isAvailable(c: Context): Boolean = when (this) {
        Sound -> c.hasMicrophone()
        Light -> c.hasSensor(Sensor.TYPE_LIGHT)
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
        Inspector -> true
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
        Meter.Level -> LevelMeter(onBack)
        Meter.Protractor -> ProtractorMeter(onBack)
        Meter.Metal -> MetalDetector(onBack)
        Meter.Compass -> CompassMeter(onBack)
        Meter.Seismo -> Seismograph(onBack)
        Meter.Baro -> Barometer(onBack)
        Meter.Vibration -> VibrationMeter(onBack)
        Meter.Climate -> ClimateMeter(onBack)
        Meter.Tacho -> Tachometer(onBack)
        Meter.Steps -> StepsMeter(onBack)
        Meter.Proximity -> ProximityMeter(onBack)
        Meter.Hinge -> HingeMeter(onBack)
        Meter.Inspector -> SensorInspector(onBack)
    }
}
