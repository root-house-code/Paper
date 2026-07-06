import XCTest
@testable import Paper

final class EditorTextOpsTests: XCTestCase {

    func testToggleWrapInsertsPairAndParksCaret() {
        let r = EditorTextOps.toggleWrap(EditorText(text: "ab", selStart: 1, selEnd: 1), "*")
        XCTAssertEqual(r.text, "a**b")
        XCTAssertEqual(r.selStart, 2)
        XCTAssertEqual(r.selEnd, 2)
    }

    func testToggleWrapWrapsSelection() {
        let r = EditorTextOps.toggleWrap(EditorText(text: "bo", selStart: 0, selEnd: 2), "*")
        XCTAssertEqual(r.text, "*bo*")
        XCTAssertEqual(r.selStart, 0)
        XCTAssertEqual(r.selEnd, 4)
    }

    func testToggleWrapUnwrapsSelection() {
        let r = EditorTextOps.toggleWrap(EditorText(text: "*bo*", selStart: 0, selEnd: 4), "*")
        XCTAssertEqual(r.text, "bo")
        XCTAssertEqual(r.selStart, 0)
        XCTAssertEqual(r.selEnd, 2)
    }

    func testPrefixLinesSingleLine() {
        let r = EditorTextOps.prefixLines(EditorText(text: "hello", selStart: 0, selEnd: 5)) { _ in "> " }
        XCTAssertEqual(r.text, "> hello")
        XCTAssertEqual(r.selStart, 7)
        XCTAssertEqual(r.selEnd, 7)
    }

    func testPrefixLinesNumberedMultiLine() {
        let r = EditorTextOps.prefixLines(EditorText(text: "a\nb", selStart: 0, selEnd: 3)) { i in "\(i + 1). " }
        XCTAssertEqual(r.text, "1. a\n2. b")
        XCTAssertEqual(r.selStart, 8)
    }

    func testPrefixLinesStartsFromLineContainingSelection() {
        // Selection begins mid-second-line; only that line is prefixed.
        let value = EditorText(text: "first\nsecond", selStart: 8, selEnd: 12)
        let r = EditorTextOps.prefixLines(value) { _ in "- " }
        XCTAssertEqual(r.text, "first\n- second")
    }
}
