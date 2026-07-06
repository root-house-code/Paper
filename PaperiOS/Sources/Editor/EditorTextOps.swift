import Foundation

/// A text value + selection, expressed in **UTF-16 offsets** to match how
/// `UITextView` reports `selectedRange` (an `NSRange`). Kotlin/Java string
/// offsets are also UTF-16, so these ports are 1:1 with `EditorScreen.kt`.
struct EditorText: Equatable {
    var text: String
    var selStart: Int
    var selEnd: Int

    init(text: String = "", selStart: Int = 0, selEnd: Int = 0) {
        self.text = text
        self.selStart = selStart
        self.selEnd = selEnd
    }
}

enum EditorTextOps {

    /// Wraps the selection in `marker`; with no selection, inserts a pair and
    /// parks the caret inside. Port of Kotlin `toggleWrap`.
    static func toggleWrap(_ value: EditorText, _ marker: String) -> EditorText {
        let ns = value.text as NSString
        let start = min(value.selStart, value.selEnd)
        let end = max(value.selStart, value.selEnd)

        let selected = ns.substring(with: NSRange(location: start, length: end - start))
        let selectedNS = selected as NSString
        let markerLen = (marker as NSString).length

        let alreadyWrapped = selectedNS.length >= markerLen * 2
            && selected.hasPrefix(marker) && selected.hasSuffix(marker)

        let pre = ns.substring(to: start)
        let post = ns.substring(from: end)

        if alreadyWrapped {
            let unwrapped = selectedNS.substring(
                with: NSRange(location: markerLen, length: selectedNS.length - markerLen * 2)
            )
            let newText = pre + unwrapped + post
            return EditorText(text: newText,
                              selStart: start,
                              selEnd: start + (unwrapped as NSString).length)
        } else {
            let newText = pre + marker + selected + marker + post
            if selectedNS.length == 0 {
                let caret = start + markerLen
                return EditorText(text: newText, selStart: caret, selEnd: caret)
            } else {
                return EditorText(text: newText, selStart: start, selEnd: end + markerLen * 2)
            }
        }
    }

    /// Prefixes every line touched by the selection, e.g. "> ", "- ", "1. ".
    /// Port of Kotlin `prefixLines`. The caret collapses to the end of the
    /// modified block (matching the Android behavior).
    static func prefixLines(_ value: EditorText, _ prefix: (Int) -> String) -> EditorText {
        let ns = value.text as NSString
        let start = min(value.selStart, value.selEnd)
        let end = max(value.selStart, value.selEnd)

        let lineStart: Int
        if start == 0 {
            lineStart = 0
        } else {
            // Last newline at or before index (start - 1); NSRange length `start`
            // covers indices [0, start-1].
            let found = ns.range(of: "\n", options: .backwards, range: NSRange(location: 0, length: start))
            lineStart = (found.location == NSNotFound) ? 0 : found.location + 1
        }

        let block = ns.substring(with: NSRange(location: lineStart, length: end - lineStart))
        let lines = block.components(separatedBy: "\n")
        let prefixed = lines.enumerated()
            .map { index, line in prefix(index) + line }
            .joined(separator: "\n")
        let added = (prefixed as NSString).length - (block as NSString).length

        let newText = ns.substring(to: lineStart) + prefixed + ns.substring(from: end)
        let caret = end + added
        return EditorText(text: newText, selStart: caret, selEnd: caret)
    }
}
