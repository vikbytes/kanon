#!/bin/bash

echo "Compressing kanon binary files..."

for file in kanon-*-linux-amd64; do
    if [ -f "$file" ] && [ ! -f "${file}.zip" ]; then
        echo "Compressing $file..."
        zip "${file}.zip" "$file"
        echo "Created ${file}.zip"
    elif [ -f "${file}.zip" ]; then
        echo "Skipping $file - zip file already exists"
    fi
done

for file in kanon-*-linux-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.zip" ]; then
        echo "Compressing $file..."
        zip "${file}.zip" "$file"
        echo "Created ${file}.zip"
    elif [ -f "${file}.zip" ]; then
        echo "Skipping $file - zip file already exists"
    fi
done

for file in kanon-*-mac-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.zip" ]; then
        echo "Compressing $file..."
        zip "${file}.zip" "$file"
        echo "Created ${file}.zip"
    elif [ -f "${file}.zip" ]; then
        echo "Skipping $file - zip file already exists"
    fi
done

echo "Compression completed!"