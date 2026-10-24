# App ProGuard rules
# Most rules are provided by libraries via their bundled rules
# and shared's consumer-rules.pro

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Diagnostic reports name exceptions along a failure's cause chain (names only, never messages). Keep those names
# readable in release builds, where "gg2" says nothing. Names only: unused exception classes are still removed.
-keepnames class * extends java.lang.Throwable
