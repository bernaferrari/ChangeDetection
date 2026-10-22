import AppKit
import ImageIO

// Shared artwork lives in artwork/app-icon. All other icon images are generated.
let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
let files = FileManager.default
let androidForegroundScale = 0.78
let androidInset = 0.19
let appleForegroundScale = 0.84

func url(_ path: String) -> URL { root.appendingPathComponent(path) }
func image(_ path: String) throws -> CGImage {
    guard let source = CGImageSourceCreateWithURL(url(path) as CFURL, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil),
          image.width == image.height else {
        throw NSError(domain: "Icons", code: 1, userInfo: [NSLocalizedDescriptionKey: "Expected square image: \(path)"])
    }
    return image
}

func render(_ size: Int, _ draw: (CGContext) -> Void) -> CGImage {
    let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8,
                            bytesPerRow: size * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    context.interpolationQuality = .high
    draw(context)
    return context.makeImage()!
}

func png(_ image: CGImage) -> Data {
    let data = NSMutableData()
    let encoder = CGImageDestinationCreateWithData(data, "public.png" as CFString, 1, nil)!
    CGImageDestinationAddImage(encoder, image, nil)
    precondition(CGImageDestinationFinalize(encoder), "PNG encoding failed")
    return data as Data
}

func write(_ data: Data, _ path: String) throws {
    let destination = url(path)
    try files.createDirectory(at: destination.deletingLastPathComponent(), withIntermediateDirectories: true)
    try data.write(to: destination, options: .atomic)
}

func write(_ text: String, _ path: String) throws { try write(Data(text.utf8), path) }
func copy(_ source: String, _ destination: String) throws { try write(Data(contentsOf: url(source)), destination) }

func run(_ tool: String, _ arguments: [String]) throws {
    let process = Process(), report = Pipe()
    process.executableURL = URL(fileURLWithPath: tool)
    process.arguments = arguments
    process.standardOutput = report; process.standardError = report
    try process.run()
    let output = report.fileHandleForReading.readDataToEndOfFile()
    process.waitUntilExit()
    guard process.terminationStatus == 0 else {
        throw NSError(domain: "Icons", code: Int(process.terminationStatus),
                      userInfo: [NSLocalizedDescriptionKey: String(decoding: output, as: UTF8.self)])
    }
}

let foreground = try image("artwork/app-icon/foreground.png")
let background = try image("artwork/app-icon/background.png")
let res = "shared/src/androidMain/res"
func frame(_ size: Int, scale: Double = 1) -> CGRect {
    let side = Double(size) * scale
    return CGRect(x: (Double(size) - side) / 2, y: (Double(size) - side) / 2, width: side, height: side)
}
func mark(_ size: Int, _ scale: Double) -> CGImage {
    render(size) { $0.draw(foreground, in: frame(size, scale: scale)) }
}
func composite(_ size: Int, scale: Double, round: Bool = false) -> CGImage {
    render(size) { context in
        if round {
            let radius = Double(size) * 0.22
            context.addPath(CGPath(roundedRect: frame(size), cornerWidth: radius, cornerHeight: radius, transform: nil))
            context.clip()
        }
        context.draw(background, in: frame(size))
        context.draw(foreground, in: frame(size, scale: scale))
    }
}

// The adaptive viewport shows the middle 72dp of a 108dp layer (a 1.5x crop).
let legacyScale = androidForegroundScale * (1 - 2 * androidInset) * 1.5
for (density, factor) in [("mdpi", 1.0), ("hdpi", 1.5), ("xhdpi", 2.0), ("xxhdpi", 3.0), ("xxxhdpi", 4.0)] {
    let layer = Int(108 * factor), legacy = Int(48 * factor)
    let directory = "\(res)/mipmap-\(density)"
    try write(png(render(layer) { $0.draw(background, in: frame(layer)) }), "\(directory)/ic_launcher_background.png")
    try write(png(mark(layer, androidForegroundScale)), "\(directory)/ic_launcher_foreground.png")
    try write(png(composite(legacy, scale: legacyScale)), "\(directory)/ic_launcher.png")
    let round = render(legacy) { context in
        context.addEllipse(in: frame(legacy)); context.clip()
        context.draw(composite(legacy, scale: legacyScale), in: frame(legacy))
    }
    try write(png(round), "\(directory)/ic_launcher_round.png")
    try write(png(mark(Int(96 * factor), legacyScale)), "\(res)/drawable-\(density)/splash_icon.png")
}
try write("""
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@mipmap/ic_launcher_foreground"
    android:inset="\(androidInset * 100)%" />

""", "\(res)/drawable/launcher_foreground.xml")
try copy("artwork/app-icon/notification.xml", "\(res)/drawable/ic_notification_change.xml")
// brand_mark.xml and brand_change.xml are mirrored by hand in ui/theme/BrandArt.kt (Kotlin vectors).

// Split the glass artwork using the rounded page silhouette, not a rectangular cut.
let page = CGMutablePath()
page.move(to: CGPoint(x: 290, y: 769)); page.addLine(to: CGPoint(x: 735, y: 769))
page.addQuadCurve(to: CGPoint(x: 830, y: 674), control: CGPoint(x: 830, y: 769))
page.addLine(to: CGPoint(x: 830, y: 344))
page.addQuadCurve(to: CGPoint(x: 735, y: 249), control: CGPoint(x: 830, y: 249))
page.addLine(to: CGPoint(x: 290, y: 249))
page.addQuadCurve(to: CGPoint(x: 195, y: 344), control: CGPoint(x: 195, y: 249))
page.addLine(to: CGPoint(x: 195, y: 674))
page.addQuadCurve(to: CGPoint(x: 290, y: 769), control: CGPoint(x: 195, y: 769))
page.closeSubpath()
let ratio = appleForegroundScale / 0.9
var transform = CGAffineTransform(a: ratio, b: 0, c: 0, d: ratio, tx: 512 * (1 - ratio), ty: 512 * (1 - ratio))
let silhouette = page.copy(using: &transform)!
let apple = "iosApp/iosApp/AppIcon.icon/Assets"
try write(png(render(1024) { $0.draw(background, in: frame(1024)) }), "\(apple)/Background.png")
for (name, corners) in [("ChangedPage", false), ("ScanCorners", true)] {
    let layer = render(1024) { context in
        if corners { context.addRect(frame(1024)) }
        context.addPath(silhouette)
        context.clip(using: .evenOdd)
        context.draw(foreground, in: frame(1024, scale: appleForegroundScale))
    }
    try write(png(layer), "\(apple)/\(name).png")
}
// icon.json preserves Icon Composer's glass, dark, and tinted appearance settings.
let compiledApple = url("build/icons/ios")
try files.createDirectory(at: compiledApple, withIntermediateDirectories: true)
try run("/usr/bin/xcrun", ["ictool", url("iosApp/iosApp/AppIcon.icon").path,
                          "--compile", compiledApple.path, "--platform", "iphoneos",
                          "--minimum-deployment-target", "26.0"])

let desktop = "shared/src/desktopMain"
let complete = composite(1024, scale: appleForegroundScale)
try write(png(complete), "\(desktop)/resources/app_icon.png")
for (path, size) in [("website/app/icon.png", 512), ("website/app/apple-icon.png", 180), ("website/public/logo.png", 512), ("webDemo/src/wasmJsMain/resources/icon.png", 512)] {
    try write(png(composite(size, scale: appleForegroundScale)), path)
}

let iconset = "build/icons/AppIcon.iconset"
try files.createDirectory(at: url(iconset), withIntermediateDirectories: true)
for size in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let suffix = scale == 2 ? "@2x" : ""
        try write(png(composite(size * scale, scale: appleForegroundScale, round: true)), "\(iconset)/icon_\(size)x\(size)\(suffix).png")
    }
}
try run("/usr/bin/iconutil", ["-c", "icns", "-o", url("\(desktop)/icons/AppIcon.icns").path, url(iconset).path])

// PNG-compressed ICO entries retain crisp taskbar artwork at every Windows size.
func little(_ value: UInt32, bytes: Int) -> Data {
    Data((0..<bytes).map { UInt8(truncatingIfNeeded: value >> ($0 * 8)) })
}
let sizes = [16, 24, 32, 48, 64, 128, 256]
let images = sizes.map { png(composite($0, scale: appleForegroundScale, round: true)) }
var ico = little(0, bytes: 2) + little(1, bytes: 2) + little(UInt32(sizes.count), bytes: 2)
var offset = 6 + sizes.count * 16
for (size, data) in zip(sizes, images) {
    ico.append(contentsOf: [UInt8(size == 256 ? 0 : size), UInt8(size == 256 ? 0 : size), 0, 0])
    ico += little(1, bytes: 2) + little(32, bytes: 2)
    ico += little(UInt32(data.count), bytes: 4) + little(UInt32(offset), bytes: 4)
    offset += data.count
}
for data in images { ico += data }
try write(ico, "\(desktop)/icons/AppIcon.ico")
print("Updated Android launcher/splash/vector icons, iOS glass layers, macOS/Windows/Linux, website, and web demo.")
