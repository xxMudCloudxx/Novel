package com.novel.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.novel.R
import com.novel.page.component.NovelText
import com.novel.page.home.HomePage
import com.novel.page.login.LoginPage
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlinx.coroutines.launch
import com.novel.page.component.rememberFlipBookAnimationController
import com.novel.page.component.GlobalFlipBookOverlay
import com.novel.rn.MviModuleType
import com.novel.rn.ReactNativePage

/**
 * 应用主页面组件
 * 
 * 小说应用的核心导航容器，采用底部导航栏 + 页面容器的经典布局：
 * 
 * 🏗️ 架构特性：
 * - HorizontalPager实现页面水平切换
 * - 全局3D翻书动画控制器集成
 * - 底部导航栏状态同步
 * - React Native混合开发支持
 * 
 * 📱 页面结构：
 * - 首页：书籍推荐和榜单展示
 * - 分类：书籍分类浏览（待实现）
 * - 福利：用户登录和活动页面
 * - 书架：个人书架管理（待实现）
 * - 我的：用户中心（React Native页面）
 * 
 * ✨ 交互特性：
 * - 防抖点击避免误触
 * - 平滑的页面切换动画
 * - 全局动画状态管理
 */
@Composable
fun MainPage() {
    // 底部导航标签配置
    val labels = listOf("首页", "分类", "福利", "书架", "我的")
    val imageId = listOf(
        R.drawable.home,        // 首页图标
        R.drawable.clarify,     // 分类图标
        R.drawable.welfare,     // 福利图标
        R.drawable.bookshelf,   // 书架图标
        R.drawable.my           // 我的图标
    )
    val pageCount = labels.size

    // 页面状态管理
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    
    // 在MainPage级别创建全局的翻书动画控制器
    // 确保所有子页面都能使用同一个动画实例
    val globalFlipBookController = rememberFlipBookAnimationController()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主要内容区域
        Column(Modifier.fillMaxSize()) {
            // 页面切换容器
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .background(color = Color(0xffF6F6F6)), // 统一背景色
                userScrollEnabled = false // 禁用手势滑动，只能通过底部导航切换
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> HomePage(
                        // 传递全局动画控制器给首页
                        globalFlipBookController = globalFlipBookController
                    )
                    2 -> LoginPage()          // 福利页面（登录相关）
                    4 -> ReactNativePage(
                        mviModuleType = MviModuleType.BRIDGE,
                    )    // 我的页面（React Native实现）
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { 
                        // 待实现页面的占位符
                        NovelText("Page Not Found") 
                    }
                }
            }

            // 底部导航栏
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.wdp),
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(0.wdp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.wdp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 渲染底部导航按钮
                    labels.forEachIndexed { index, _ ->
                        NavButton(
                            onClick = {
                                scope.launch { 
                                    pagerState.animateScrollToPage(index) 
                                }
                            },
                            isSelect = pagerState.currentPage == index,
                            text = labels[index],
                            id = imageId[index]
                        )
                    }
                }
            }
        }
        
        // 全局翻书动画覆盖层 - 放置在最顶层确保正确渲染
        GlobalFlipBookOverlay(
            controller = globalFlipBookController
        )
    }
}

/**
 * 底部导航按钮组件
 * 
 * 单个导航项的UI实现，包含图标和文字：
 * - 选中状态的视觉反馈
 * - 防抖点击处理
 * - 图标颜色状态管理
 * 
 * @param onClick 点击事件回调
 * @param isSelect 是否为选中状态
 * @param text 导航项文字
 * @param id 图标资源ID
 */
@Composable
fun NavButton(
    onClick: () -> Unit = {},
    isSelect: Boolean = false,
    text: String,
    id: Int
) {
    // 根据选中状态确定颜色
    val color = if (isSelect) NovelColors.NovelText else NovelColors.NovelTextGray
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .debounceClickable(onClick = onClick), // 防抖点击
    ) {
        // 导航图标
        Image(
            painter = painterResource(id = id),
            contentDescription = text, // 使用文字作为无障碍描述
            modifier = Modifier.size(20.wdp, 20.wdp),
            colorFilter = ColorFilter.tint(color)
        )
        
        // 导航文字
        NovelText(
            text = text,
            fontSize = 10.ssp,
            lineHeight = 14.ssp,
            fontWeight = if (isSelect) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.wdp),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

/**
 * 主页面预览组件
 * 
 * 用于Android Studio的设计时预览
 */
@Preview
@Composable
fun MainPagePreview() {
    NovelTheme {
        MainPage()
    }
}
