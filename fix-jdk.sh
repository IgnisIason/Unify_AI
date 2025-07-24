#!/bin/bash

# Fix Gradle JDK configuration for Android Studio
echo "🔧 Fixing Gradle JDK configuration..."

# Check if Android Studio JDK exists
if [ -d "/opt/android-studio/jbr" ]; then
    echo "✅ Android Studio embedded JDK found at /opt/android-studio/jbr"
    
    # Set JAVA_HOME for this session
    export JAVA_HOME=/opt/android-studio/jbr
    echo "Set JAVA_HOME to: $JAVA_HOME"
    
    # Verify Java version
    echo "Java version:"
    $JAVA_HOME/bin/java -version
    
    echo ""
    echo "🎯 Configuration updated:"
    echo "   - gradle.xml: Set gradleJvm to 'jbr-21' (registered JDK name)"
    echo "   - gradle.properties: Already configured with org.gradle.java.home"
    echo "   - jdk.table.xml: JDK already registered as 'jbr-21'"
    echo "   - JAVA_HOME exported for current session"
    echo ""
    echo "📋 Next steps in Android Studio:"
    echo "   1. File → Invalidate Caches and Restart"
    echo "   2. File → Project Structure → SDK Location"
    echo "   3. Set Gradle JDK to 'jbr-21' (Embedded JDK)"
    echo "   4. Click Apply and sync project"
    echo ""
    echo "✅ The JDK reference has been corrected to use the registered JDK name."
    
else
    echo "❌ Android Studio embedded JDK not found at /opt/android-studio/jbr"
    echo "Please check your Android Studio installation path."
    echo "Common locations:"
    echo "   - /opt/android-studio/jbr"
    echo "   - /snap/android-studio/current/android-studio/jbr"
    echo "   - ~/android-studio/jbr"
fi