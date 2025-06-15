#!/bin/bash

check_graalvm() {
    if [ -n "$GRAALVM_HOME" ] && [ -d "$GRAALVM_HOME" ]; then
        JAVA_HOME="$GRAALVM_HOME"
        echo "Using GraalVM from GRAALVM_HOME: $GRAALVM_HOME"
    elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        if "$JAVA_HOME/bin/java" -version 2>&1 | grep -i "GraalVM" > /dev/null; then
            echo "Using GraalVM from JAVA_HOME: $JAVA_HOME"
        else
            echo "WARNING: Current JAVA_HOME is not pointing to GraalVM."
            echo "Native image building requires GraalVM."
            echo "Please install GraalVM and set JAVA_HOME or GRAALVM_HOME."
            echo "You can download GraalVM from: https://www.graalvm.org/downloads/"
            exit 1
        fi
    else
        if command -v java > /dev/null; then
            if java -version 2>&1 | grep -i "GraalVM" > /dev/null; then
                JAVA_HOME=$(dirname $(dirname $(which java)))
                echo "Using GraalVM from PATH: $JAVA_HOME"
            else
                echo "WARNING: Current Java installation is not GraalVM."
                echo "Native image building requires GraalVM."
                echo "Please install GraalVM and set JAVA_HOME or GRAALVM_HOME."
                echo "You can download GraalVM from: https://www.graalvm.org/downloads/"
                exit 1
            fi
        else
            echo "ERROR: Java not found. Please install GraalVM JDK."
            echo "You can download GraalVM from: https://www.graalvm.org/downloads/"
            exit 1
        fi
    fi

    if ! "$JAVA_HOME/bin/native-image" --version > /dev/null 2>&1; then
        echo "ERROR: native-image tool not found in GraalVM installation."
        echo "Please make sure you have installed the native-image component:"
        echo "$JAVA_HOME/bin/gu install native-image"
        exit 1
    fi

    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"
}

check_graalvm

CURRENT_ARCH=$(uname -m)
VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')

echo "Building Kanon native image version $VERSION for $CURRENT_ARCH."
echo "Using GraalVM at: $JAVA_HOME"
echo "This may take a few minutes..."

./gradlew clean nativeCompile

cp "build/native/nativeCompile/kanon-$VERSION" "./kanon-${VERSION}"
chmod +x "./kanon-${VERSION}"
echo "Binary: ./kanon-${VERSION}"
echo "Native build completed successfully!"
