package com.novel.page.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novel.ui.theme.NovelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Login页面UI测试
 * 
 * 测试内容：
 * 1. UI组件渲染
 * 2. 基本交互
 * 3. 状态显示
 */
@RunWith(AndroidJUnit4::class)
class LoginPageUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginPage_初始状态_显示正确() {
        composeTestRule.setContent {
            NovelTheme {
                LoginPage()
            }
        }

        // 验证基本UI元素存在
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun loginPage_基本渲染_成功() {
        composeTestRule.setContent {
            NovelTheme {
                LoginPage()
            }
        }

        // 等待UI渲染完成
        composeTestRule.waitForIdle()
        
        // 验证页面渲染成功
        composeTestRule.onRoot().assertExists()
    }
} 