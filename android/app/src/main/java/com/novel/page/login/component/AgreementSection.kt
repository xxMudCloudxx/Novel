package com.novel.page.login.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.PingFangFamily
import androidx.compose.foundation.text.BasicText
import com.novel.utils.wdp
import com.novel.utils.ssp

/**
 * 录页 - 协议
 * @param operator 运营商
 * @param isChecked 是否选中
 * @param onCheckedChange 选中状态改变回调
 * @param onTelServiceClick 点击 “认证服务协议”
 * @param onUserAgreementClick 点击 “用户协议”
 * @param onRegisterAgreementClick 点击 “注册协议”
 */
@Composable
fun AgreementSection(
    operator: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTelServiceClick: () -> Unit,
    onUserAgreementClick: () -> Unit,
    onRegisterAgreementClick: () -> Unit
) {
    //—— ① 预取主题色，避免在 Canvas DrawScope 内重复调用 @Composable Getter
    val checkedColor = NovelColors.NovelMainLight
    val uncheckedColor = NovelColors.NovelTextGray

    //—— ② 统一文本样式
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = NovelColors.NovelTextGray,
        fontSize = 12.ssp,
        lineHeight = 15.ssp,
        fontWeight = FontWeight.Thin,
        fontFamily = PingFangFamily
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.wdp)
            .padding(horizontal = 40.wdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.wdp)
                    .clickable { onCheckedChange(!isChecked) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (isChecked) {
                        // 实心圆
                        drawCircle(color = checkedColor, style = Fill)
                    } else {
                        // 空心圆
                        drawCircle(
                            color = uncheckedColor,
                            style = Stroke(width = 1.wdp.toPx())
                        )
                    }
                }
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = NovelColors.NovelSecondaryBackground,
                        modifier = Modifier.size(10.wdp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.wdp))

            // 构建第一行可点击文本
            val annotatedText = buildAnnotatedString {
                append("已阅读并同意 ")
                addLink(
                    LinkAnnotation.Clickable(
                        tag = "TEL",
                        styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                        linkInteractionListener = { onTelServiceClick() }
                    ),
                    start = this.length,
                    end = this.length + operator.length + "认证服务协议".length
                )
                append(operator + "认证服务协议")
                append(" 以及")
            }
            BasicText(
                text = annotatedText,
                style = textStyle,
                maxLines = 1
            )
        }

        // 构建第二行可点击文本
        val policyText = buildAnnotatedString {
            addLink(
                LinkAnnotation.Clickable(
                    tag = "USER",
                    styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                    linkInteractionListener = { onUserAgreementClick() }
                ),
                start = this.length,
                end = this.length + "用户协议".length
            )
            append("用户协议")
            append(" 和 ")
            addLink(
                LinkAnnotation.Clickable(
                    tag = "REGISTER",
                    styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                    linkInteractionListener = { onRegisterAgreementClick() }
                ),
                start = this.length,
                end = this.length + "注册协议".length
            )
            append("注册协议")
        }
        BasicText(
            text = policyText,
            style = textStyle
        )
    }
}

@Preview
@Composable
fun AgreementSectionPreview() {
    AgreementSection(
        operator = "中国电信",
        isChecked = true,
        onCheckedChange = {},
        onTelServiceClick = {},
        onUserAgreementClick = {},
        onRegisterAgreementClick = {}
    )
}