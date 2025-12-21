# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the build.gradle file.

# Keep public API classes
-keep public class com.yourcompany.a11y.ChartA11y { *; }
-keep public class com.yourcompany.a11y.ChartA11y$Builder { *; }
-keep public class com.yourcompany.a11y.DescType { *; }
-keep public class com.yourcompany.a11y.A11yDelegate { *; }
