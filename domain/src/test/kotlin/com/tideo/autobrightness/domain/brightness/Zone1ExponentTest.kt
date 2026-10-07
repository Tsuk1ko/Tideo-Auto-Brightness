package com.tideo.autobrightness.domain.brightness

import com.tideo.autobrightness.domain.wizard.CurveSuggestionEngine
import com.tideo.autobrightness.domain.wizard.CurveSuggestionInput
import com.tideo.autobrightness.domain.wizard.OverridePoint
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Zone1ExponentTest {
    private val engine = BrightnessEngine()

    @Test
    fun defaultExponent_keepsOriginalSquareRoot() {
        val curve = BrightnessCurveConfig()
        assertEquals(0.5, curve.zone1Exponent)
        for (lux in listOf(0.0, 0.1, 1.0, 3.0, 10.0, 34.99)) {
            assertEquals(curve.form1A * sqrt(lux), engine.mapLuxToBrightness(lux, curve))
        }
    }

    @Test
    fun exponents_preserveEndpointAndUpperZones_andRiseMonotonically() {
        val original = BrightnessCurveConfig()
        val endpoint = original.form1A * sqrt(original.zone1End)
        for (exponent in listOf(0.5, 1.0, 1.5, 2.0, 3.0)) {
            val curve = original.copy(zone1Exponent = exponent)
            var previous = 0.0
            for (step in 0..100) {
                val lux = step * curve.zone1End / 100.0
                val brightness = engine.mapLuxToBrightness(lux, curve)
                assertTrue(brightness.isFinite() && brightness >= previous)
                assertEquals(endpoint * (lux / curve.zone1End).pow(exponent), brightness, 1e-10)
                previous = brightness
            }
            assertEquals(endpoint, previous, 1e-10)
            assertEquals(endpoint, engine.mapLuxToBrightness(curve.zone1End - 1e-7, curve), 1e-5)
            for (lux in listOf(curve.zone1End, 100.0, curve.zone2End, 30_000.0)) {
                assertEquals(engine.mapLuxToBrightness(lux, original), engine.mapLuxToBrightness(lux, curve))
            }
        }
        assertTrue(engine.mapLuxToBrightness(10.0, original.copy(zone1Exponent = 2.0)) <
            engine.mapLuxToBrightness(10.0, original.copy(zone1Exponent = 1.5)))
    }

    @Test
    fun wizard_fitsWithFixedExponent_andCarriesItIntoAppliedCurve() {
        val curve = BrightnessCurveConfig(zone1Exponent = 1.5)
        val points = listOf(0.1, 1.0, 3.0, 6.0, 12.0, 20.0, 35.0, 60.0, 100.0, 300.0,
            1_000.0, 3_000.0, 10_000.0, 15_000.0, 30_000.0).map { lux ->
            val brightness = if (lux < curve.zone1End) {
                curve.form1A * sqrt(curve.zone1End) * (lux / curve.zone1End).pow(curve.zone1Exponent)
            } else {
                engine.mapLuxToBrightness(lux, curve)
            }
            OverridePoint(lux, brightness)
        }
        val result = assertNotNull(CurveSuggestionEngine.suggest(CurveSuggestionInput(points, curve)))
        assertEquals(curve.zone1Exponent, result.zone1Exponent)
        assertTrue(result.diagnosticsLog.contains("Zone1Exponent (fixed): 1.500"))
        val applied = CurveSuggestionEngine.applyToLiveCurve(result, curve.copy(zone1Exponent = 2.0))
        assertEquals(curve.zone1Exponent, applied.zone1Exponent)
        assertEquals(applied.form2A, engine.mapLuxToBrightness(applied.zone1End, applied), 1e-10)
        val errors = points.filter { it.lux < applied.zone1End }.map { point ->
            kotlin.math.abs(engine.mapLuxToBrightness(point.lux, applied) - point.brightness)
        }
        assertTrue(errors.isNotEmpty() && errors.max() < 1.0)
    }
}
