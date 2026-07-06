import XCTest
@testable import Paper

final class MarkdownStylerTests: XCTestCase {

    func testBoldSpan() {
        XCTAssertEqual(
            findMarkdownSpans("*bold*"),
            [MarkdownSpan(range: NSRange(location: 0, length: 6), style: .bold)]
        )
    }

    func testPlainTextHasNoSpans() {
        XCTAssertTrue(findMarkdownSpans("plain text").isEmpty)
    }

    func testInlineCodeSpan() {
        XCTAssertEqual(
            findMarkdownSpans("a `code` b"),
            [MarkdownSpan(range: NSRange(location: 2, length: 6), style: .code)]
        )
    }

    func testCodeBlockBlocksInnerInlineMarkers() {
        // Backticks inside a fenced block must not be treated as inline code.
        XCTAssertEqual(
            findMarkdownSpans("```a`b```"),
            [MarkdownSpan(range: NSRange(location: 0, length: 9), style: .codeBlock)]
        )
    }

    func testAdjacentMarkersWithEmptyContentIgnored() {
        XCTAssertTrue(findMarkdownSpans("**").isEmpty)
    }

    func testMixedStyles() {
        // "*b* _i_" → bold at 0..2, italic at 4..6.
        XCTAssertEqual(
            findMarkdownSpans("*b* _i_"),
            [
                MarkdownSpan(range: NSRange(location: 0, length: 3), style: .bold),
                MarkdownSpan(range: NSRange(location: 4, length: 3), style: .italic),
            ]
        )
    }
}
