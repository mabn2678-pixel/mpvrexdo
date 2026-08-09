package com.finalplayer.app.ui.theme

import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.finalplayer.app.data.preferences.AppearancePreferences
import org.koin.compose.koinInject

fun getThemeColorScheme(
    preset: String,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val bgDark = if (isAmoled) Color.Black else Color(0xFF121212)
    val surfaceDark = if (isAmoled) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)
    val surfaceVariantDark = if (isAmoled) Color(0xFF141414) else Color(0xFF2C2C2C)

    return when (preset.lowercase()) {
        "kanagawa" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF98BB6C),
                onPrimary = Color(0xFF131F0B),
                primaryContainer = Color(0xFF2D3C21),
                onPrimaryContainer = Color(0xFFC8E6A6),
                secondary = Color(0xFF7E9CD8),
                onSecondary = Color(0xFF0C192E),
                secondaryContainer = Color(0xFF22324D),
                onSecondaryContainer = Color(0xFFC3D6FA),
                tertiary = Color(0xFFE6C384),
                onTertiary = Color(0xFF281E00),
                tertiaryContainer = Color(0xFF433400),
                onTertiaryContainer = Color(0xFFFFDF9E),
                background = bgDark,
                onBackground = Color(0xFFDCD7BA),
                surface = surfaceDark,
                onSurface = Color(0xFFDCD7BA),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFC8C093)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF4D6930),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFCEECAD),
                onPrimaryContainer = Color(0xFF0F2000),
                secondary = Color(0xFF3A5283),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD7E2FF),
                onSecondaryContainer = Color(0xFF001A40),
                tertiary = Color(0xFF725B23),
                background = Color(0xFFF2ECDF),
                onBackground = Color(0xFF1F1C18),
                surface = Color(0xFFFFFBF2),
                onSurface = Color(0xFF1F1C18),
                surfaceVariant = Color(0xFFE6E2D5),
                onSurfaceVariant = Color(0xFF49473E)
            )
        }
        "catppuccin" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFCBA6F7),
                onPrimary = Color(0xFF33145A),
                primaryContainer = Color(0xFF4C2A78),
                onPrimaryContainer = Color(0xFFECCFFF),
                secondary = Color(0xFF89B4FA),
                onSecondary = Color(0xFF002B5C),
                secondaryContainer = Color(0xFF1B4278),
                onSecondaryContainer = Color(0xFFD2E3FF),
                tertiary = Color(0xFFF5E0DC),
                onTertiary = Color(0xFF3E1C18),
                tertiaryContainer = Color(0xFF58322C),
                onTertiaryContainer = Color(0xFFFFDAD5),
                background = bgDark,
                onBackground = Color(0xFFCDD6F4),
                surface = surfaceDark,
                onSurface = Color(0xFFCDD6F4),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFA6ADC8)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF8839EF),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFECCFFF),
                onPrimaryContainer = Color(0xFF2C0067),
                secondary = Color(0xFF1E66F5),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD2E3FF),
                onSecondaryContainer = Color(0xFF001944),
                background = Color(0xFFEFF1F5),
                onBackground = Color(0xFF4C4F69),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF4C4F69),
                surfaceVariant = Color(0xFFE6E9EF),
                onSurfaceVariant = Color(0xFF6C6F85)
            )
        }
        "cloudflare" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFF48120),
                onPrimary = Color(0xFF4D1F00),
                primaryContainer = Color(0xFF6F3000),
                onPrimaryContainer = Color(0xFFFFDCC4),
                secondary = Color(0xFFFAAE42),
                onSecondary = Color(0xFF452B00),
                secondaryContainer = Color(0xFF634000),
                onSecondaryContainer = Color(0xFFFFDEAC),
                tertiary = Color(0xFFD96B00),
                onTertiary = Color(0xFF3E1A00),
                tertiaryContainer = Color(0xFF5B2900),
                onTertiaryContainer = Color(0xFFFFDCC2),
                background = bgDark,
                onBackground = Color(0xFFEDE0D4),
                surface = surfaceDark,
                onSurface = Color(0xFFEDE0D4),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFD0C4B8)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFE65100),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDCC4),
                onPrimaryContainer = Color(0xFF300B00),
                secondary = Color(0xFFF57C00),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFDCC2),
                onSecondaryContainer = Color(0xFF2E1500),
                background = Color(0xFFFFF8F2),
                onBackground = Color(0xFF241A12),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF241A12),
                surfaceVariant = Color(0xFFF5E6DA),
                onSurfaceVariant = Color(0xFF52443A)
            )
        }
        "doom" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFF5555),
                onPrimary = Color(0xFF5A000A),
                primaryContainer = Color(0xFF800013),
                onPrimaryContainer = Color(0xFFFFDAD8),
                secondary = Color(0xFFFF79C6),
                onSecondary = Color(0xFF530036),
                secondaryContainer = Color(0xFF75004E),
                onSecondaryContainer = Color(0xFFFFD8EC),
                tertiary = Color(0xFFBD93F9),
                onTertiary = Color(0xFF34006B),
                tertiaryContainer = Color(0xFF4C108B),
                onTertiaryContainer = Color(0xFFEDDCFF),
                background = bgDark,
                onBackground = Color(0xFFF8F8F2),
                surface = surfaceDark,
                onSurface = Color(0xFFF8F8F2),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFBFBFC2)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFC62828),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD8),
                onPrimaryContainer = Color(0xFF410004),
                secondary = Color(0xFFAD1457),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFD8EC),
                onSecondaryContainer = Color(0xFF3B0022),
                background = Color(0xFFFFF5F5),
                onBackground = Color(0xFF2C1515),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF2C1515),
                surfaceVariant = Color(0xFFF4DADA),
                onSurfaceVariant = Color(0xFF534343)
            )
        }
        "cotton_candy" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFF79C6),
                onPrimary = Color(0xFF58003A),
                primaryContainer = Color(0xFF7B0053),
                onPrimaryContainer = Color(0xFFFFD8EC),
                secondary = Color(0xFF8BE9FD),
                onSecondary = Color(0xFF00363F),
                secondaryContainer = Color(0xFF004F5C),
                onSecondaryContainer = Color(0xFFB5F0FF),
                tertiary = Color(0xFFBD93F9),
                onTertiary = Color(0xFF34006B),
                tertiaryContainer = Color(0xFF4C108B),
                onTertiaryContainer = Color(0xFFEDDCFF),
                background = bgDark,
                onBackground = Color(0xFFF8F8F2),
                surface = surfaceDark,
                onSurface = Color(0xFFF8F8F2),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFD4C2D0)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD81B60),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD8EC),
                onPrimaryContainer = Color(0xFF3B0024),
                secondary = Color(0xFF00ACC1),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFB5F0FF),
                onSecondaryContainer = Color(0xFF001F24),
                background = Color(0xFFFFF0F5),
                onBackground = Color(0xFF2A151E),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF2A151E),
                surfaceVariant = Color(0xFFF3DDE6),
                onSurfaceVariant = Color(0xFF514349)
            )
        }
        "green_apple" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF50FA7B),
                onPrimary = Color(0xFF003914),
                primaryContainer = Color(0xFF005220),
                onPrimaryContainer = Color(0xFF72FF98),
                secondary = Color(0xFF8BE9FD),
                onSecondary = Color(0xFF00363F),
                secondaryContainer = Color(0xFF004F5C),
                onSecondaryContainer = Color(0xFFB5F0FF),
                tertiary = Color(0xFF50FA7B),
                onTertiary = Color(0xFF003914),
                tertiaryContainer = Color(0xFF005220),
                onTertiaryContainer = Color(0xFF72FF98),
                background = bgDark,
                onBackground = Color(0xFFE2F3E5),
                surface = surfaceDark,
                onSurface = Color(0xFFE2F3E5),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFBDC9BF)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF2E7D32),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB0F2BC),
                onPrimaryContainer = Color(0xFF002107),
                secondary = Color(0xFF00838F),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFB2EBF2),
                onSecondaryContainer = Color(0xFF001F23),
                background = Color(0xFFF1F8F3),
                onBackground = Color(0xFF152218),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF152218),
                surfaceVariant = Color(0xFFDDE6DF),
                onSurfaceVariant = Color(0xFF414942)
            )
        }
        "gruvbox" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFABD2F),
                onPrimary = Color(0xFF3F2E00),
                primaryContainer = Color(0xFF5B4300),
                onPrimaryContainer = Color(0xFFFFDF9E),
                secondary = Color(0xFFFE8019),
                onSecondary = Color(0xFF4A1A00),
                secondaryContainer = Color(0xFF6A2A00),
                onSecondaryContainer = Color(0xFFFFDBC9),
                tertiary = Color(0xFF83A598),
                onTertiary = Color(0xFF003632),
                tertiaryContainer = Color(0xFF004F49),
                onTertiaryContainer = Color(0xFFA0D1C4),
                background = bgDark,
                onBackground = Color(0xFFEBDBB2),
                surface = surfaceDark,
                onSurface = Color(0xFFEBDBB2),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFD5C4A1)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFB57614),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDF9E),
                onPrimaryContainer = Color(0xFF271900),
                secondary = Color(0xFFAF3A03),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFDBC9),
                onSecondaryContainer = Color(0xFF3B0900),
                background = Color(0xFFFBF1C7),
                onBackground = Color(0xFF3C3836),
                surface = Color(0xFFF2E5BC),
                onSurface = Color(0xFF3C3836),
                surfaceVariant = Color(0xFFE5D5AC),
                onSurfaceVariant = Color(0xFF504945)
            )
        }
        "lavender" -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFD0BCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                secondary = Color(0xFFCCC2DC),
                onSecondary = Color(0xFF332D41),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                tertiary = Color(0xFFEFB8C8),
                onTertiary = Color(0xFF492532),
                tertiaryContainer = Color(0xFF633B48),
                onTertiaryContainer = Color(0xFFFFD8E4),
                background = bgDark,
                onBackground = Color(0xFFE6E1E5),
                surface = surfaceDark,
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFCAC4D0)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6750A4),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                secondary = Color(0xFF625B71),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1D192B),
                background = Color(0xFFFEF7FF),
                onBackground = Color(0xFF1D1B20),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFE7E0EC),
                onSurfaceVariant = Color(0xFF49454F)
            )
        }
        else -> if (isDark) {
            darkColorScheme(
                primary = MpvGreenPrimary,
                onPrimary = Color(0xFF00391A),
                primaryContainer = Color(0xFF005227),
                onPrimaryContainer = Color(0xFF7CFFB2),
                secondary = MpvGreenSecondary,
                onSecondary = Color(0xFF003913),
                secondaryContainer = Color(0xFF005222),
                onSecondaryContainer = Color(0xFF6BFF9C),
                tertiary = MpvGreenDark,
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF003913),
                onTertiaryContainer = Color(0xFF7CFFB2),
                background = bgDark,
                onBackground = OnDarkTextPrimary,
                surface = surfaceDark,
                onSurface = OnDarkTextPrimary,
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = Color(0xFFC4C7C5)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF00893A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF7CFFB2),
                onPrimaryContainer = Color(0xFF00210C),
                secondary = Color(0xFF007931),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF6BFF9C),
                onSecondaryContainer = Color(0xFF00210A),
                tertiary = MpvGreenDark,
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFC2F8CE),
                onTertiaryContainer = Color(0xFF00210C),
                background = LightBackground,
                onBackground = Color(0xFF1A1C1A),
                surface = LightSurface,
                onSurface = Color(0xFF1A1C1A),
                surfaceVariant = LightSurfaceVariant,
                onSurfaceVariant = Color(0xFF424942)
            )
        }
    }
}

@Composable
fun FinalPlayerTheme(
    appearancePrefs: AppearancePreferences = koinInject(),
    content: @Composable () -> Unit
) {
    val themeMode by appearancePrefs.themeMode.asFlow().collectAsState(initial = "system")
    val themePreset by appearancePrefs.themePreset.asFlow().collectAsState(initial = "default")
    val isAmoled by appearancePrefs.amoledMode.asFlow().collectAsState(initial = true)

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDark
    }

    val colorScheme = getThemeColorScheme(themePreset, isDark, isAmoled)

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? androidx.activity.ComponentActivity
        androidx.compose.runtime.DisposableEffect(isDark) {
            activity?.enableEdgeToEdge(
                statusBarStyle = if (isDark) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                },
                navigationBarStyle = if (isDark) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
            )
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

