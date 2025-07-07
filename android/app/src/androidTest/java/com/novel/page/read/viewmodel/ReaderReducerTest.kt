//package com.novel.page.read.viewmodel
//
//import androidx.compose.ui.unit.Density
//import androidx.compose.ui.unit.IntSize
//import com.google.common.truth.Truth.assertThat
//import com.novel.page.read.components.ReaderSettings
//import org.junit.Test
//import org.junit.runner.RunWith
//import androidx.test.ext.junit.runners.AndroidJUnit4
//
//@RunWith(AndroidJUnit4::class)
//class ReaderReducerTest {
//
//    private val reducer = ReaderReducer()
//
//    @Test
//    fun `reduce InitReader intent should set isLoading to true and store bookId`() {
//        // Given
//        val initialState = ReaderState()
//        val intent = ReaderIntent.InitReader("book1", "chapter1")
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.newState.isLoading).isTrue()
//        assertThat(result.newState.error).isNull()
//        assertThat(result.newState.bookId).isEqualTo("book1")
//    }
//
//    @Test
//    fun `reduce Retry intent should set isLoading to true and clear error`() {
//        // Given
//        val initialState = ReaderState(error = "Some error")
//        val intent = ReaderIntent.Retry
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.newState.isLoading).isTrue()
//        assertThat(result.newState.error).isNull()
//    }
//
//    @Test
//    fun `reduce PageFlip intent should produce haptic feedback effect`() {
//        // Given
//        val initialState = ReaderState()
//        val intent = ReaderIntent.PageFlip(FlipDirection.NEXT)
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.effect).isInstanceOf(ReaderEffect.TriggerHapticFeedback::class.java)
//    }
//
//    @Test
//    fun `reduce UpdateSettings intent should update settings and produce brightness effect if changed`() {
//        // Given
//        val initialState = ReaderState(readerSettings = ReaderSettings.getDefault().copy(brightness = 0.5f))
//        val newSettings = ReaderSettings.getDefault().copy(brightness = 0.8f)
//        val intent = ReaderIntent.UpdateSettings(newSettings)
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.newState.readerSettings.brightness).isEqualTo(0.8f)
//        assertThat(result.effect).isInstanceOf(ReaderEffect.SetBrightness::class.java)
//        assertThat((result.effect as ReaderEffect.SetBrightness).brightness).isEqualTo(0.8f)
//    }
//
//    @Test
//    fun `reduce UpdateContainerSize intent should update size and density`() {
//        // Given
//        val initialState = ReaderState()
//        val newSize = IntSize(1080, 1920)
//        val newDensity = Density(2.0f)
//        val intent = ReaderIntent.UpdateContainerSize(newSize, newDensity)
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.newState.containerSize).isEqualTo(newSize)
//        assertThat(result.newState.density).isEqualTo(newDensity)
//    }
//
//    @Test
//    fun `reduce ToggleMenu intent should toggle menu and hide other panels`() {
//        // Given
//        val initialState = ReaderState(isChapterListVisible = true, isSettingsPanelVisible = true)
//        val intent = ReaderIntent.ToggleMenu(true)
//
//        // When
//        val result = reducer.reduce(initialState, intent)
//
//        // Then
//        assertThat(result.newState.isMenuVisible).isTrue()
//        assertThat(result.newState.isChapterListVisible).isFalse()
//        assertThat(result.newState.isSettingsPanelVisible).isFalse()
//    }
//}