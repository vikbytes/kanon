#!/bin/bash

VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
echo "Building version: $VERSION"

ARM64_IMAGE_NAME="kanon-linux-arm64"

echo "Building for Linux ARM64 architecture..."
docker build --platform=linux/arm64 --memory=8g --memory-swap=10g --build-arg BUILDKIT_INLINE_CACHE=1 -t ${ARM64_IMAGE_NAME} -f Dockerfile.native .

echo "Extracting binaries..."

ARM64_BINARY="kanon-${VERSION}-linux-arm64"

docker create --name kanon-temp-arm64 ${ARM64_IMAGE_NAME}
docker cp kanon-temp-arm64:/app/kanon ./"${ARM64_BINARY}"
docker rm kanon-temp-arm64
chmod +x ./"${ARM64_BINARY}"

echo "Linux ARM64 binary: ./${ARM64_BINARY}"
