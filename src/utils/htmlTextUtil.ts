/**
 * 将 HTML 中的 &nbsp; 替换为空格，
 * 将 <br/><br/> 替换为换行符
 * @param {string} text - 原始含有 HTML 实体的字符串
 * @returns {string} - 清理后的纯文本
 */
export function cleanHtml(text: string) {
    return text
      // 把所有 &nbsp; 都替换为普通空格
      .replace(/&nbsp;/g, ' ')
      // 把所有 <br/><br/> 替换为\n
      .replace(/<br\/><br\/>/g, '\n');
  }