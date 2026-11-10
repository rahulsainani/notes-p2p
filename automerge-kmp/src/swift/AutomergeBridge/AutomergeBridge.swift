import Automerge
import Foundation

// Kotlin/Native sees only Objective-C APIs, and automerge-swift is pure Swift.
// This class is the @objc surface that iosMain calls; it mirrors the common AutomergeDocument API.
@objcMembers public class AMBDocument: NSObject {
    let document: Document

    override public init() {
        document = Document()
        super.init()
    }

    init(document: Document) {
        self.document = document
        super.init()
    }

    // A factory, not a throwing init: Kotlin sees a failed Objective-C init as a null constructor result.
    public class func fromBytes(_ bytes: Data) throws -> AMBDocument {
        AMBDocument(document: try Document(bytes))
    }

    public func putString(_ key: String, value: String) throws {
        try document.put(obj: ObjId.ROOT, key: key, value: .String(value))
    }

    // Objective-C cannot express "throws" with an optional result, so a read error is a missing value.
    // Reads on the root map fail only for a corrupt document, which loading already rejects.
    public func getString(_ key: String) -> String? {
        guard case let .Scalar(.String(value)) = try? document.get(obj: ObjId.ROOT, key: key) else {
            return nil
        }
        return value
    }

    public func save() -> Data {
        document.save()
    }

    public func merge(_ other: AMBDocument) throws {
        try document.merge(other: other.document)
    }
}
