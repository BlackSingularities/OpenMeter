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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.blacksingularities.openmeter.core.hasMicrophone
import com.blacksingularities.openmeter.core.hasSensor
import com.blacksingularities.openmeter.ui.Palette

/** Grupy mierników z własnym kolorem akcentu. */
enum class Category(val label: String, val color: Color) {
    Acoustics("Dźwięk i światło", Palette.Amber),
    Position("Położenie", Palette.Cyan),
    Magnetism("Magnetyzm", Palette.Rose),
    Motion("Ruch", Palette.Mint),
    Environment("Otoczenie", Palette.Sky),
    Device("Urządzenie", Palette.Violet),
}

/** Wszystkie mierniki. Na ekranie głównym widać tylko te, dla których telefon ma czujnik. */
enum class Meter(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: Category,
) {
    Sound("sound", "Decybelomierz", "Poziom dźwięku, widmo, stroik", Icons.Rounded.GraphicEq, Category.Acoustics),
    Light("light", "Luksomierz", "Natężenie światła i ekspozycja", Icons.Rounded.WbSunny, Category.Acoustics),
    Level("level", "Poziomica", "Libella 2D i pozioma", Icons.Rounded.Straighten, Category.Position),
    Protractor("protractor", "Kątomierz", "Kąt krawędzi i nachylenie", Icons.Rounded.Architecture, Category.Position),
    Metal("metal", "Wykrywacz metalu", "Pole magnetyczne w µT", Icons.Rounded.Sensors, Category.Magnetism),
    Compass("compass", "Kompas", "Azymut i pole magnetyczne", Icons.Rounded.Explore, Category.Magnetism),
    Seismo("seismo", "Sejsmograf", "Drgania podłoża z akcelerometru", Icons.Rounded.Waves, Category.Motion),
    Baro("baro", "Barometr", "Ciśnienie, wysokość, trend", Icons.Rounded.Speed, Category.Environment),
    Vibration("vibration", "Drgania w podróży", "Komfort jazdy autem i pociągiem", Icons.Rounded.Train, Category.Motion),
    Climate("climate", "Klimat", "Temperatura, wilgotność, punkt rosy", Icons.Rounded.Thermostat, Category.Environment),
    Tacho("tacho", "Obrotomierz", "Prędkość obrotu z żyroskopu", Icons.Rounded.Autorenew, Category.Motion),
    Steps("steps", "Krokomierz", "Kroki i kadencja", Icons.AutoMirrored.Rounded.DirectionsWalk, Category.Motion),
    Proximity("proximity", "Zbliżenie", "Czujnik zbliżeniowy i licznik gestów", Icons.Rounded.PanTool, Category.Device),
    Hinge("hinge", "Kąt zawiasu", "Rozłożenie składanego telefonu", Icons.AutoMirrored.Rounded.MenuBook, Category.Device),
    Inspector("inspector", "Wszystkie czujniki", "Lista i odczyty na żywo", Icons.Rounded.Memory, Category.Device),
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

    val accent: Color get() = category.color

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
