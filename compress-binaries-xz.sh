#!/bin/bash

echo "Compressing kanon binary files with XZ compression..."

for file in kanon-*-linux-amd64; do
    if [ -f "$file" ] && [ ! -f "${file}.xz" ]; then
        echo "Compressing $file..."
        xz -z -k -9 "$file"
        echo "Created ${file}.xz"
    elif [ -f "${file}.xz" ]; then
        echo "Skipping $file - xz file already exists"
    fi
done

for file in kanon-*-linux-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.xz" ]; then
        echo "Compressing $file..."
        xz -z -k -9 "$file"
        echo "Created ${file}.xz"
    elif [ -f "${file}.xz" ]; then
        echo "Skipping $file - xz file already exists"
    fi
done

for file in kanon-*-mac-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.xz" ]; then
        echo "Compressing $file..."
        xz -z -k -9 "$file"
        echo "Created ${file}.xz"
    elif [ -f "${file}.xz" ]; then
        echo "Skipping $file - xz file already exists"
    fi
done

echo "XZ compression completed!"