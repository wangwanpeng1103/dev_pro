/**
 * 复制文本到剪贴板；非 HTTPS 或浏览器拒绝 Clipboard API 时使用兼容方案。
 */
export async function copyTextToClipboard(text: string) {
  if (!text) return false
  if (window.isSecureContext && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // 部分浏览器或远程访问环境会拒绝 Clipboard API，继续走兼容复制。
    }
  }
  return fallbackCopyText(text)
}

function fallbackCopyText(text: string) {
  const activeElement = document.activeElement
  const selection = document.getSelection()
  const selectedRange = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.width = '1px'
  textarea.style.height = '1px'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'
  document.body.appendChild(textarea)
  textarea.focus({ preventScroll: true })
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)
  try {
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
    if (selectedRange && selection) {
      selection.removeAllRanges()
      selection.addRange(selectedRange)
    }
    if (activeElement instanceof HTMLElement) activeElement.focus({ preventScroll: true })
  }
}