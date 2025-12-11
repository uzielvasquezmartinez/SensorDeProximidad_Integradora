import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val context = LocalContext.current
    return calculateWindowSizeClass(context as Activity)
}

@Composable
fun adaptiveDp(
    compact: Dp,
    medium: Dp,
    expanded: Dp
): Dp {
    val sizeClass = rememberWindowSizeClass()
    return when (sizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> compact
        WindowWidthSizeClass.Medium -> medium
        else -> expanded
    }
}

@Composable
fun isTablet(): Boolean {
    val sizeClass = rememberWindowSizeClass()
    return sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
}
