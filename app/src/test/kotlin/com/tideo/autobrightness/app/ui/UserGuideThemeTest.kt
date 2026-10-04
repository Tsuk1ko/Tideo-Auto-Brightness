package com.tideo.autobrightness.app.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.children
import com.tideo.autobrightness.app.ui.screens.UserGuideContent
import com.tideo.autobrightness.app.ui.theme.TideoTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class UserGuideThemeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lightTheme_appliesToHtmlAndWebViewBackground() {
        render(mutableStateOf(false))
        compose.runOnIdle { assertPalette(webView(), false) }
    }

    @Test
    fun darkTheme_appliesToHtmlAndWebViewBackground() {
        render(mutableStateOf(true))
        compose.runOnIdle { assertPalette(webView(), true) }
    }

    @Test
    fun themeChanges_updateTheExistingWebViewInBothDirections() {
        val dark = mutableStateOf(false)
        render(dark)
        lateinit var initialView: WebView
        compose.runOnIdle {
            initialView = webView()
            assertPalette(initialView, false)
            dark.value = true
        }
        compose.runOnIdle {
            assertSame(initialView, webView())
            assertPalette(initialView, true)
            dark.value = false
        }
        compose.runOnIdle {
            assertSame(initialView, webView())
            assertPalette(initialView, false)
        }
    }

    private fun render(dark: State<Boolean>) {
        compose.setContent {
            TideoTheme(darkTheme = dark.value) { UserGuideContent(onBack = {}) }
        }
    }

    private fun webView(): WebView = checkNotNull(
        compose.activity.findViewById<View>(android.R.id.content).findWebView(),
    )

    private fun View.findWebView(): WebView? =
        if (this is WebView) this else (this as? ViewGroup)?.children?.firstNotNullOfOrNull { it.findWebView() }

    private fun assertPalette(view: WebView, dark: Boolean) {
        val background = if (dark) "#333333" else "#f6f8f7"
        val foreground = if (dark) "#ececec" else "#1a1c1b"
        val scheme = if (dark) "dark" else "light"
        val shadow = shadowOf(view)
        assertEquals(Color.parseColor(background), shadow.backgroundColor)
        val html = checkNotNull(shadow.lastLoadDataWithBaseURL).data.lowercase(Locale.ROOT)
        assertTrue(html.contains("color-scheme: $scheme;"), "HTML color scheme should follow the theme")
        assertTrue(html.contains("background:$background; color:$foreground;"), "HTML body should follow the theme")
        assertFalse(view.settings.javaScriptEnabled)
    }
}
