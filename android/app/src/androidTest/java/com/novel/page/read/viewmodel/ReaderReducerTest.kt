 package com.novel.page.read.viewmodel

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.novel.page.read.components.ReaderSettings
import org.junit.Test

class ReaderReducerTest {

    private val reducer = ReaderReducer()

    @Test
    fun `reduce InitReader intent should set isLoading to true and store bookId`() {
        // Given
        val initialState = ReaderState()
        val intent = ReaderIntent.InitReader("book1", "chapter1")

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isLoading).isTrue()
        assertThat(result.newState.error).isNull()
        assertThat(result.newState.bookId).isEqualTo("book1")
    }

    @Test
    fun `reduce Retry intent should set isLoading to true`() {
        // Given
        val initialState = ReaderState(error = "Some error")
        val intent = ReaderIntent.Retry

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isLoading).isTrue()
        assertThat(result.newState.error).isNull()
    }

    @Test
    fun `reduce PageFlip intent should trigger haptic feedback effect`() {
        // Given
        val initialState = ReaderState()
        val intent = ReaderIntent.PageFlip(FlipDirection.NEXT)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.effect).isInstanceOf(ReaderEffect.TriggerHapticFeedback::class.java)
    }

    @Test
    fun `reduce NextChapter intent should set isSwitchingChapter to true`() {
        // Given
        val initialState = ReaderState()
        val intent = ReaderIntent.NextChapter

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isSwitchingChapter).isTrue()
    }

    @Test
    fun `reduce SwitchToChapter intent should set isSwitchingChapter and hide chapter list`() {
        // Given
        val initialState = ReaderState(isChapterListVisible = true)
        val intent = ReaderIntent.SwitchToChapter("chapter2")

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isSwitchingChapter).isTrue()
        assertThat(result.newState.isChapterListVisible).isFalse()
    }

    @Test
    fun `reduce UpdateSettings should update settings and produce brightness effect if changed`() {
        // Given
        val initialSettings = ReaderSettings.getDefault().copy(brightness = 0.5f)
        val newSettings = initialSettings.copy(brightness = 0.8f, fontSize = 20)
        val initialState = ReaderState(readerSettings = initialSettings)
        val intent = ReaderIntent.UpdateSettings(newSettings)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.readerSettings).isEqualTo(newSettings)
        assertThat(result.effect).isInstanceOf(ReaderEffect.SetBrightness::class.java)
        assertThat((result.effect as ReaderEffect.SetBrightness).brightness).isEqualTo(0.8f)
    }
    
    @Test
    fun `reduce UpdateSettings should not produce effect if brightness not changed`() {
        // Given
        val initialSettings = ReaderSettings.getDefault().copy(brightness = 0.5f)
        val newSettings = initialSettings.copy(fontSize = 20) // Brightness is same
        val initialState = ReaderState(readerSettings = initialSettings)
        val intent = ReaderIntent.UpdateSettings(newSettings)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.readerSettings).isEqualTo(newSettings)
        assertThat(result.effect).isNull()
    }

    @Test
    fun `reduce UpdateContainerSize should update container size and density`() {
        // Given
        val initialState = ReaderState()
        val newSize = IntSize(1080, 1920)
        val newDensity = Density(2.0f, 1.0f)
        val intent = ReaderIntent.UpdateContainerSize(newSize, newDensity)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.containerSize).isEqualTo(newSize)
        assertThat(result.newState.density).isEqualTo(newDensity)
    }

    @Test
    fun `reduce ToggleMenu(true) should show menu and hide other panels`() {
        // Given
        val initialState = ReaderState(isChapterListVisible = true, isSettingsPanelVisible = true)
        val intent = ReaderIntent.ToggleMenu(true)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isMenuVisible).isTrue()
        assertThat(result.newState.isChapterListVisible).isFalse()
        assertThat(result.newState.isSettingsPanelVisible).isFalse()
    }

    @Test
    fun `reduce ShowChapterList(true) should show chapter list and hide other panels`() {
        // Given
        val initialState = ReaderState(isMenuVisible = true, isSettingsPanelVisible = true)
        val intent = ReaderIntent.ShowChapterList(true)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isChapterListVisible).isTrue()
        assertThat(result.newState.isMenuVisible).isFalse()
        assertThat(result.newState.isSettingsPanelVisible).isFalse()
    }

    @Test
    fun `reduce ShowSettingsPanel(true) should show settings and hide other panels`() {
        // Given
        val initialState = ReaderState(isMenuVisible = true, isChapterListVisible = true)
        val intent = ReaderIntent.ShowSettingsPanel(true)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertThat(result.newState.isSettingsPanelVisible).isTrue()
        assertThat(result.newState.isMenuVisible).isFalse()
        assertThat(result.newState.isChapterListVisible).isFalse()
    }
}
