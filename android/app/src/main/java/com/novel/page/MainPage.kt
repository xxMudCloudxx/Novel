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
import com.novel.utils.NavViewModel
import com.novel.page.component.rememberFlipBookAnimationController
import com.novel.page.component.FlipBookTrigger
import com.novel.page.component.GlobalFlipBookOverlay

@Composable
fun MainPage() {
    // 页面内容列表——注意中间第三项是 ChatScreen
    val labels = listOf("首页", "分类", "福利", "书架", "我的")
    val imageId = listOf(
        R.drawable.home,
        R.drawable.clarify,
        R.drawable.welfare,
        R.drawable.bookshelf,
        R.drawable.my
    )
    val pageCount = labels.size

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    
    // 在MainPage级别创建全局的翻书动画控制器
    val globalFlipBookController = rememberFlipBookAnimationController()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主要内容
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).background(color = Color(0xffF6F6F6)),
                userScrollEnabled = false
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> HomePage(
                        // 传递全局动画控制器给HomePage
                        globalFlipBookController = globalFlipBookController
                    )
                    2 -> LoginPage()
                    4 -> ReactNativePage()
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { NovelText("Page Not Found") }
                }
            }

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
                    labels.forEachIndexed { index, _ ->
                        NavButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            isSelect = pagerState.currentPage == index,
                            text = labels[index],
                            id = imageId[index]
                        )
                    }
                }
            }
        }
        
        // 全局翻书动画覆盖层 - 在最顶层
        GlobalFlipBookOverlay(
            controller = globalFlipBookController
        )
    }
    
    // 全局翻书动画触发器
    FlipBookTrigger(
        controller = globalFlipBookController,
        onNavigate = { bookId ->
            NavViewModel.navigateToBookDetail(bookId, fromRank = true)
        }
    )
}

@Composable
fun NavButton(
    onClick: () -> Unit = {},
    isSelect: Boolean = false,
    text: String,
    id: Int
) {
    val color = if (isSelect) NovelColors.NovelText else NovelColors.NovelTextGray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .debounceClickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = id),
            contentDescription = "back",
            modifier = Modifier
                .size(20.wdp, 20.wdp),
            colorFilter = ColorFilter.tint(color)
        )
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

@Preview
@Composable
fun MainPagePreview() {
    NovelTheme {
        MainPage()
    }
}
