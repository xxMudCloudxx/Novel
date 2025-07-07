//package com.novel.page.read.viewmodel
//
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.google.common.truth.Truth.assertThat
//import org.junit.Test
//import org.junit.runner.RunWith
//import kotlin.system.measureTimeMillis
//
//@RunWith(AndroidJUnit4::class)
//class ReaderPerformanceTest {
//
//    @Test
//    fun `state update performance should be fast`() {
//        // Given
//        val initialState = ReaderState()
//        val iterations = 1000
//
//        // When
//        val time = measureTimeMillis {
//            var state = initialState
//            repeat(iterations) {
//                state = state.copy(version = state.version + 1)
//            }
//        }
//
//        // Then
//        // A simple assertion to ensure state copying is not a bottleneck.
//        // Should be well under 50ms for 1000 iterations.
//        assertThat(time).isLessThan(50)
//        println("State update time for $iterations iterations: ${time}ms")
//    }
//
//    @Test
//    fun `reducer performance should be fast`() {
//        // Given
//        val reducer = ReaderReducer()
//        val initialState = ReaderState()
//        val intent = ReaderIntent.ToggleMenu(true)
//        val iterations = 1000
//
//        // When
//        val time = measureTimeMillis {
//            var state = initialState
//            repeat(iterations) {
//                state = reducer.reduce(state, intent).newState
//            }
//        }
//
//        // Then
//        // Reducer should be a pure, fast function.
//        // Should be well under 50ms for 1000 iterations.
//        assertThat(time).isLessThan(50)
//        println("Reducer processing time for $iterations iterations: ${time}ms")
//    }
//}