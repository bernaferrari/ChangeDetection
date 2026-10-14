Replace `foreground.png` and `background.png`, then run `swift tools/update-icons.swift` from the repository root on a Mac with Xcode installed.

That updates every raster icon: Android adaptive and legacy launchers, splash screens, iOS glass layers, macOS ICNS, Windows ICO, Linux/desktop resources, and both web projects. Size and spacing are controlled at the top of the generator. iOS appearance and glass settings stay in `AppIcon.icon/icon.json`; the command validates them with Xcode's official icon tool.

Edit `notification.xml` to update Android notifications and themed icons together. `brand_mark.xml` and `brand_change.xml` provide the two in-app vector layers. The same command copies those sources into the app.
