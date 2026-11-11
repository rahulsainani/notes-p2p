package app.droidster.automerge

internal const val FIXTURE_VALUE = "Saved by one wrapper, loaded by all"

// An emoji first, so a wrapper that counts positions in other units breaks the marks.
internal const val FIXTURE_TEXT = "😀 text"
internal const val FIXTURE_TEXT_LENGTH = 6

/**
 * The content version that a new fixture gets. Raise it when the fixture document gains a data type.
 */
internal const val FIXTURE_CONTENT = "types"

/**
 * What each fixture content version holds, as a [snapshot].
 */
internal val fixtureContents: Map<String, Map<String, Any?>> = mapOf(
    "string" to mapOf("title" to FIXTURE_VALUE),
    "types" to mapOf(
        "string" to FIXTURE_VALUE,
        "integer" to 1_762_600_000_000,
        "boolean" to true,
        "null" to null,
        "map" to mapOf(
            "text" to TextSnapshot(
                FIXTURE_TEXT,
                listOf(
                    Mark(0, 2, "bold", AutomergeValue.Bool(true)),
                    Mark(0, FIXTURE_TEXT_LENGTH, "heading", AutomergeValue.Integer(2)),
                ),
            ),
        ),
    ),
)

/**
 * Base64 documents saved by each wrapper version (key: [bindingId] + `/` + content version).
 * Never remove an entry.
 */
@Suppress("MaxLineLength") // Each fixture is one Base64 string, kept whole on its line.
internal val fixtures: Map<String, String> = mapOf(
    "automerge-java-0.0.9/string" to
        "hW9KgxvWjdcAlQEBEH9/5r2n4/H6JRYsV6vUThcBe5o09aUCrvYVjlFeIFkWiXa9GFx8i+AqAJKRBpd7FXMGAQIDAhMCIwJAAlYCCBUHIQIjAjQBQgJWA1cjgAECfwB/AX8BfwB/AH8HfwV0aXRsZX8AfwEBfwF/tgRTYXZlZCBieSBvbmUgd3JhcHBlciwgbG9hZGVkIGJ5IGFsbH8AAA==",
    "automerge-swift-0.7.2/string" to
        "hW9Kg0XJ9ZMAlQEBEPaJdVhgYs1Hb8tFQn1u/R0B7W58WNcfMfQirPTqceBiDjcFAWM8t7eZrL8WmeC5r5QGAQIDAhMCIwJAAlYCCBUHIQIjAjQBQgJWA1cjgAECfwB/AX8BfwB/AH8HfwV0aXRsZX8AfwEBfwF/tgRTYXZlZCBieSBvbmUgd3JhcHBlciwgbG9hZGVkIGJ5IGFsbH8AAA==",
    "automerge-java-0.0.9/types" to
        "hW9KgzY8vA0AsAIBEOeBM78qxAd1Wa5Q/GsxUYgBdWjYqq31zLI4iHZy6/yhGOC+4CgMyvVHq8s9Gx5MZ94GAQIDAhMCIwJAAlYCDgEEAgYRBBMKFSghAiMRNAJCElYSVzOAAQKUAQOlARJ/AH8BfxB/AH8AfwcABQsAAAV/BQoGAAkHAAAGAwB9BwEABAF6B2Jvb2xlYW4HaW50ZWdlcgNtYXAEbnVsbAZzdHJpbmcEdGV4dAAKEAB0A38Df30FCX56AQZ7AwF/BAYKAgF/AAIBfwQCBwIBfwcEAX8HfgJjAgB5tgQAFAJGFgAEFn8AgLSWmaYzU2F2ZWQgYnkgb25lIHdyYXBwZXIsIGxvYWRlZCBieSBhbGwC8J+YgCB0ZXh0EAAKAQUABn4HaGVhZGluZwRib2xkAAgA",
    "automerge-swift-0.7.2/types" to
        "hW9Kgx2USKUAsAIBEH00aEwoR3MbxZKxas8t/eYBVbzPLhAkW27Ogq22LE7IhE1bi2rgpxAUL8hYvwzW4WMGAQIDAhMCIwJAAlYCDgEEAgYRBBMKFSghAiMRNAJCElYSVzOAAQKUAQOlARJ/AH8BfxB/AH8AfwcABQsAAAV/BQoGAAkHAAAGAwB9BwEABAF6B2Jvb2xlYW4HaW50ZWdlcgNtYXAEbnVsbAZzdHJpbmcEdGV4dAAKEAB0A38Df30FCX56AQZ7AwF/BAYKAgF/AAIBfwQCBwIBfwcEAX8HfgJkAgB5tgQAFAJGFgAEFn8AgLSWmaYzU2F2ZWQgYnkgb25lIHdyYXBwZXIsIGxvYWRlZCBieSBhbGwC8J+YgCB0ZXh0EAAKAQUABn4HaGVhZGluZwRib2xkAAgA",
)
