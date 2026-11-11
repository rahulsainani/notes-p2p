import Automerge
import Foundation

// Kotlin/Native sees only Objective-C APIs, and automerge-swift is pure Swift.
// These classes are the @objc surface that iosMain calls; they mirror the common AutomergeDocument API.

enum AMBError: Error {
    case invalidActor
    case invalidHeads
}

@objcMembers public class AMBObjId: NSObject {
    let id: ObjId

    init(id: ObjId) {
        self.id = id
        super.init()
    }

    public class func root() -> AMBObjId {
        AMBObjId(id: ObjId.ROOT)
    }
}

// Objective-C has no enum with payloads, so a value is a kind plus one filled field.
@objcMembers public class AMBValue: NSObject {
    public static let kindMissing = 0
    public static let kindString = 1
    public static let kindInteger = 2
    public static let kindBoolean = 3
    public static let kindNull = 4
    public static let kindMap = 5
    public static let kindList = 6
    public static let kindText = 7
    public static let kindOther = 8

    public let kind: Int
    public let string: String?
    public let integer: Int64
    public let boolean: Bool
    public let objId: AMBObjId?

    init(kind: Int, string: String? = nil, integer: Int64 = 0, boolean: Bool = false, objId: AMBObjId? = nil) {
        self.kind = kind
        self.string = string
        self.integer = integer
        self.boolean = boolean
        self.objId = objId
        super.init()
    }

    convenience init(scalar: ScalarValue) {
        switch scalar {
        case let .String(value): self.init(kind: AMBValue.kindString, string: value)
        case let .Int(value): self.init(kind: AMBValue.kindInteger, integer: value)
        case let .Uint(value): self.init(kind: AMBValue.kindInteger, integer: Int64(clamping: value))
        case let .Boolean(value): self.init(kind: AMBValue.kindBoolean, boolean: value)
        case .Null: self.init(kind: AMBValue.kindNull)
        default: self.init(kind: AMBValue.kindOther)
        }
    }

    convenience init(value: Value) {
        switch value {
        case let .Scalar(scalar): self.init(scalar: scalar)
        case let .Object(id, .Map): self.init(kind: AMBValue.kindMap, objId: AMBObjId(id: id))
        case let .Object(id, .List): self.init(kind: AMBValue.kindList, objId: AMBObjId(id: id))
        case let .Object(id, .Text): self.init(kind: AMBValue.kindText, objId: AMBObjId(id: id))
        }
    }
}

@objcMembers public class AMBMark: NSObject {
    public let start: Int64
    public let end: Int64
    public let name: String
    public let value: AMBValue

    init(mark: Mark) {
        start = Int64(mark.start)
        end = Int64(mark.end)
        name = mark.name
        value = AMBValue(scalar: mark.value)
        super.init()
    }
}

@objcMembers public class AMBDocument: NSObject {
    let document: Document

    init(document: Document, actor: Data?) throws {
        self.document = document
        super.init()
        if let actor {
            guard let actorId = ActorId(data: actor) else { throw AMBError.invalidActor }
            document.actor = actorId
        }
    }

    // Factories, not throwing inits: Kotlin sees a failed Objective-C init as a null constructor result.
    public class func create(_ actor: Data?) throws -> AMBDocument {
        try AMBDocument(document: Document(), actor: actor)
    }

    public class func fromBytes(_ bytes: Data, actor: Data?) throws -> AMBDocument {
        try AMBDocument(document: try Document(bytes), actor: actor)
    }

    public func actorId() -> String {
        document.actor.description.lowercased()
    }

    public func heads() -> [String] {
        document.heads().map(\.debugDescription).sorted()
    }

    // Objective-C cannot express "throws" with an optional result, so a missing key is kindMissing.
    public func get(_ obj: AMBObjId, key: String) throws -> AMBValue {
        guard let value = try document.get(obj: obj.id, key: key) else {
            return AMBValue(kind: AMBValue.kindMissing)
        }
        return AMBValue(value: value)
    }

    public func getAll(_ obj: AMBObjId, key: String) throws -> [AMBValue] {
        try document.getAll(obj: obj.id, key: key).map { AMBValue(value: $0) }
    }

    public func keys(_ obj: AMBObjId) -> [String] {
        document.keys(obj: obj.id)
    }

    public func text(_ obj: AMBObjId) throws -> String {
        try document.text(obj: obj.id)
    }

    public func marks(_ obj: AMBObjId) throws -> [AMBMark] {
        try document.marks(obj: obj.id).map { AMBMark(mark: $0) }
    }

    public func putString(_ obj: AMBObjId, key: String, value: String) throws {
        try document.put(obj: obj.id, key: key, value: .String(value))
    }

    public func putInteger(_ obj: AMBObjId, key: String, value: Int64) throws {
        try document.put(obj: obj.id, key: key, value: .Int(value))
    }

    public func putBoolean(_ obj: AMBObjId, key: String, value: Bool) throws {
        try document.put(obj: obj.id, key: key, value: .Boolean(value))
    }

    public func putNull(_ obj: AMBObjId, key: String) throws {
        try document.put(obj: obj.id, key: key, value: .Null)
    }

    public func putObject(_ obj: AMBObjId, key: String, kind: Int) throws -> AMBObjId {
        let type: ObjType = kind == AMBValue.kindText ? .Text : kind == AMBValue.kindList ? .List : .Map
        return AMBObjId(id: try document.putObject(obj: obj.id, key: key, ty: type))
    }

    public func delete(_ obj: AMBObjId, key: String) throws {
        try document.delete(obj: obj.id, key: key)
    }

    public func spliceText(_ obj: AMBObjId, start: Int64, delete: Int64, text: String) throws {
        try document.spliceText(obj: obj.id, start: UInt64(start), delete: delete, value: text)
    }

    public func markBoolean(
        _ obj: AMBObjId, start: Int64, end: Int64, name: String, value: Bool, expand: Int
    ) throws {
        try mark(obj, start: start, end: end, name: name, value: .Boolean(value), expand: expand)
    }

    public func markInteger(
        _ obj: AMBObjId, start: Int64, end: Int64, name: String, value: Int64, expand: Int
    ) throws {
        try mark(obj, start: start, end: end, name: name, value: .Int(value), expand: expand)
    }

    // automerge-swift has no unmark; a null mark removes the mark, as in the Rust core.
    public func unmark(_ obj: AMBObjId, start: Int64, end: Int64, name: String, expand: Int) throws {
        try mark(obj, start: start, end: end, name: name, value: .Null, expand: expand)
    }

    public func save() -> Data {
        document.save()
    }

    public func merge(_ other: AMBDocument) throws {
        try document.merge(other: other.document)
    }

    // heads: the 32-byte change hashes, joined. automerge-swift gives the fork a new random actor.
    public func forkAt(_ heads: Data) throws -> AMBDocument {
        guard let hashes = heads.heads() else { throw AMBError.invalidHeads }
        return try AMBDocument(document: try document.forkAt(heads: hashes), actor: nil)
    }

    // The expand order matches the Kotlin ExpandMark enum: before, after, both, none.
    private func mark(
        _ obj: AMBObjId, start: Int64, end: Int64, name: String, value: ScalarValue, expand: Int
    ) throws {
        let expandMarks: [ExpandMark] = [ExpandMark.before, ExpandMark.after, ExpandMark.both, ExpandMark.none]
        try document.mark(
            obj: obj.id, start: UInt64(start), end: UInt64(end), expand: expandMarks[expand], name: name,
            value: value
        )
    }
}
