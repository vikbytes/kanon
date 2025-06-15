#!/bin/bash

VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
echo "Building version: $VERSION"

AMD64_IMAGE_NAME="kanon-linux-amd64"

echo "Building for Linux AMD64 architecture..."
docker build --platform=linux/amd64 --memory=8g --memory-swap=10g --build-arg BUILDKIT_INLINE_CACHE=1 -t ${AMD64_IMAGE_NAME} -f Dockerfile.native .

echo "Extracting binaries..."

AMD64_BINARY="kanon-${VERSION}-linux-amd64"

docker create --name kanon-temp-amd64 ${AMD64_IMAGE_NAME}
docker cp kanon-temp-amd64:/app/kanon ./"${AMD64_BINARY}"
docker rm kanon-temp-amd64
chmod +x ./"${AMD64_BINARY}"

echo "Linux AMD64 binary: ./${AMD64_BINARY}"
