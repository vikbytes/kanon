#!/bin/bash

set -e

VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
echo "Building version: $VERSION"

IMAGE_NAME="vikbytes/kanon"

if ! docker buildx version > /dev/null 2>&1; then
    echo "Docker buildx is not available. Please install Docker buildx."
    echo "See: https://docs.docker.com/buildx/working-with-buildx/"
    exit 1
fi

if ! docker buildx inspect multiarch-builder > /dev/null 2>&1; then
    echo "Creating a new Docker buildx builder instance..."
    docker buildx create --name multiarch-builder --driver docker-container --bootstrap
fi

docker buildx use multiarch-builder

echo "Building and pushing multi-architecture Java image..."
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag ${IMAGE_NAME}:${VERSION} \
    --tag ${IMAGE_NAME}:latest \
    --push \
    -f Dockerfile .


echo "Multi-architecture Docker images have been built and pushed to Docker Hub."
echo "Java image: ${IMAGE_NAME}:${VERSION} and ${IMAGE_NAME}:latest"