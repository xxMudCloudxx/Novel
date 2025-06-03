package com.novel.page.component.pagecurl.page

/**
 * 标记实验性的PageCurl API
 * 
 * 使用此注解标记的API可能在未来版本中发生变化或被移除
 * 
 * @author Novel Team
 */
@RequiresOptIn(
    message = "This is an experimental API for PageCurl and may change in the future.",
    level = RequiresOptIn.Level.WARNING
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalPageCurlApi 