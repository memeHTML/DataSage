package com.retailiq.datasage.ui.components

import com.github.mikephil.charting.data.LineDataSet
import com.retailiq.datasage.data.api.ForecastPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartComponentsTest {

    // Explicit color ints to avoid android.graphics.Color.parseColor() in JVM tests
    private val navyColor = 0xFF1A237E.toInt()
    private val tealColor = 0xFF009688.toInt()
    private val amberColor = 0xFFFFB300.toInt()
    private val bandColor = 0x801A237E.toInt()
    private val bandFillColor = 0x401A237E.toInt()

    @Test
    fun buildForecastDataSets_withValidBounds_rendersConfidenceBand() {
        val historical = listOf(HistoricalPoint("Day1", 100.0))
        val forecastWithBounds = listOf(
            ForecastPoint("Day2", 110.0, 90.0, 130.0)
        )

        val datasets = buildForecastDataSets(
            historical = historical,
            forecast = forecastWithBounds,
            navyColor = navyColor,
            tealColor = tealColor,
            amberColor = amberColor,
            bandColor = bandColor,
            bandFillColor = bandFillColor
        )

        assertEquals(3, datasets.size)
        val boundarySet = datasets.find { it.label == "Confidence Band" }
        assertTrue("Confidence Band dataset should exist", boundarySet != null)
    }

    @Test
    fun buildForecastDataSets_zeroBounds_skipsConfidenceBand() {
        val historical = listOf(HistoricalPoint("Day1", 100.0))
        val forecastNoBounds = listOf(
            ForecastPoint("Day2", 110.0, 0.0, 0.0)
        )

        val datasets = buildForecastDataSets(
            historical = historical,
            forecast = forecastNoBounds,
            navyColor = navyColor,
            tealColor = tealColor,
            amberColor = amberColor,
            bandColor = bandColor,
            bandFillColor = bandFillColor
        )

        // Historical + Forecast only, no Confidence Band
        assertEquals(2, datasets.size)
        val boundarySet = datasets.find { it.label == "Confidence Band" }
        assertTrue("Confidence Band dataset should NOT exist", boundarySet == null)
    }

    @Test
    fun buildForecastDataSets_emptyHistorical_onlyForecast() {
        val forecast = listOf(
            ForecastPoint("Day1", 110.0, 90.0, 130.0)
        )

        val datasets = buildForecastDataSets(
            historical = emptyList(),
            forecast = forecast,
            navyColor = navyColor,
            tealColor = tealColor,
            amberColor = amberColor,
            bandColor = bandColor,
            bandFillColor = bandFillColor
        )

        // Forecast + Confidence Band only (no historical)
        assertEquals(2, datasets.size)
        assertTrue("Should have Forecast dataset", datasets.any { it.label == "Forecast" })
        assertTrue("Should have Confidence Band dataset", datasets.any { it.label == "Confidence Band" })
    }

    @Test
    fun buildForecastDataSets_bothEmpty_returnsEmpty() {
        val datasets = buildForecastDataSets(
            historical = emptyList(),
            forecast = emptyList(),
            navyColor = navyColor,
            tealColor = tealColor,
            amberColor = amberColor,
            bandColor = bandColor,
            bandFillColor = bandFillColor
        )

        assertTrue("Datasets should be empty", datasets.isEmpty())
    }
}
