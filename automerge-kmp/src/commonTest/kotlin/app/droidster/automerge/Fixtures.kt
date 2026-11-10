package app.droidster.automerge

internal const val FIXTURE_KEY = "title"
internal const val FIXTURE_VALUE = "Saved by one wrapper, loaded by all"

/** Base64 documents saved by each wrapper version (key: [bindingId]). Never remove an entry. */
internal val fixtures: Map<String, String> = mapOf(
    "automerge-java-0.0.9" to
        "hW9KgxvWjdcAlQEBEH9/5r2n4/H6JRYsV6vUThcBe5o09aUCrvYVjlFeIFkWiXa9GFx8i+AqAJKRBpd7FXMGAQIDAhMCIwJAAlYCCBUHIQIjAjQBQgJWA1cjgAECfwB/AX8BfwB/AH8HfwV0aXRsZX8AfwEBfwF/tgRTYXZlZCBieSBvbmUgd3JhcHBlciwgbG9hZGVkIGJ5IGFsbH8AAA==",
    "automerge-swift-0.7.2" to
        "hW9Kg0XJ9ZMAlQEBEPaJdVhgYs1Hb8tFQn1u/R0B7W58WNcfMfQirPTqceBiDjcFAWM8t7eZrL8WmeC5r5QGAQIDAhMCIwJAAlYCCBUHIQIjAjQBQgJWA1cjgAECfwB/AX8BfwB/AH8HfwV0aXRsZX8AfwEBfwF/tgRTYXZlZCBieSBvbmUgd3JhcHBlciwgbG9hZGVkIGJ5IGFsbH8AAA==",
)
