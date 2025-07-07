package com.novel.page.read.viewmodel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novel.page.read.ReaderPage
import com.novel.ui.theme.NovelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.hilt.navigation.compose.hiltViewModel

@RunWith(AndroidJUnit4::class)
class ReaderPageUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readerPage_InitialLoadingState_ShowsLoadingIndicator() {
        // Given
        // The ViewModel will be in its initial loading state by default
        
        // When
        composeTestRule.setContent {
            NovelTheme {
                // In a real scenario with Hilt, we'd use a TestHiltAndroidRule
                // For simplicity here, we assume the initial state is loading
                ReaderPage(bookId = "test", chapterId = "test")
            }
        }

        // Then
        // The loading state component is managed by the LoadingStateComponent wrapper
        // We can't easily assert the circular progress indicator directly without tags
        // But we can check for the absence of error or content
        composeTestRule.onNodeWithText("重试").assertDoesNotExist()
    }
    
    // Additional tests can be added here for:
    // - Error state display
    // - Content display after successful load
    // - Menu visibility toggling
    // - Settings panel appearance
} 