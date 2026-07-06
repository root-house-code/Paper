import Foundation

/// The Slack-flavored markup styles Paper supports, matching the Android
/// `Markdown.kt` rules. Markers stay visible in both the editor and read views.
enum MarkdownStyle: Equatable {
    case bold           // *bold*
    case italic         // _italic_
    case strikethrough  // ~strike~
    case code           // `code`
    case codeBlock      // ```block```
}

/// A styled range within the raw markup, in **UTF-16 offsets** (`NSRange`) so it
/// applies directly to an `NSAttributedString` in the UITextView editor.
struct MarkdownSpan: Equatable {
    let range: NSRange
    let style: MarkdownStyle
}

private struct InlineRule {
    let marker: String
    let style: MarkdownStyle
}

private let inlineRules: [InlineRule] = [
    InlineRule(marker: "*", style: .bold),
    InlineRule(marker: "_", style: .italic),
    InlineRule(marker: "~", style: .strikethrough),
    InlineRule(marker: "`", style: .code),
]

/// Pure port of Kotlin `findSpans`: code blocks first (inline rules don't apply
/// inside them), then paired inline markers. Kept free of UIKit so it is unit
/// testable; the attribute application (fonts/colors) lives in the UITextView
/// interop layer.
func findMarkdownSpans(_ text: String) -> [MarkdownSpan] {
    let ns = text as NSString
    let length = ns.length
    guard length > 0 else { return [] }

    var spans: [MarkdownSpan] = []
    var blocked = [Bool](repeating: false, count: length)

    // Code blocks first.
    var i = 0
    while i < length {
        let openRange = ns.range(of: "```", range: NSRange(location: i, length: length - i))
        if openRange.location == NSNotFound { break }
        let open = openRange.location
        let afterOpen = open + 3
        let closeRange = ns.range(of: "```", range: NSRange(location: afterOpen, length: length - afterOpen))
        if closeRange.location == NSNotFound { break }
        let close = closeRange.location
        spans.append(MarkdownSpan(range: NSRange(location: open, length: close + 3 - open), style: .codeBlock))
        for j in open..<(close + 3) { blocked[j] = true }
        i = close + 3
    }

    // Inline rules over the non-blocked regions.
    for rule in inlineRules {
        var from = 0
        while from < length {
            let openRange = ns.range(of: rule.marker, range: NSRange(location: from, length: length - from))
            if openRange.location == NSNotFound { break }
            let open = openRange.location
            if blocked[open] { from = open + 1; continue }

            let searchFrom = open + 1
            if searchFrom >= length { break }
            let closeRange = ns.range(of: rule.marker, range: NSRange(location: searchFrom, length: length - searchFrom))
            if closeRange.location == NSNotFound { break }
            let close = closeRange.location
            if blocked[close] || close == open + 1 {
                from = close
                continue
            }
            spans.append(MarkdownSpan(range: NSRange(location: open, length: close + 1 - open), style: rule.style))
            from = close + 1
        }
    }

    return spans
}
