# Consumer ProGuard rules for chart-a11y-runtime
# These rules will be applied to consumer apps that use this library

# Keep public API classes
-keep public class com.yourcompany.a11y.ChartA11y { *; }
-keep public class com.yourcompany.a11y.ChartA11y$Builder { *; }
-keep public class com.yourcompany.a11y.DescType { *; }
-keep public class com.yourcompany.a11y.A11yDelegate { *; }
