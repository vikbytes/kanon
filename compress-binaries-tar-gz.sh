#!/bin/bash

echo "Compressing kanon binary files with tar.gz compression..."

for file in kanon-*-linux-amd64; do
    if [ -f "$file" ] && [ ! -f "${file}.tar.gz" ]; then
        echo "Compressing $file..."
        tar -czf "${file}.tar.gz" "$file"
        echo "Created ${file}.tar.gz"
    elif [ -f "${file}.tar.gz" ]; then
        echo "Skipping $file - tar.gz file already exists"
    fi
done

for file in kanon-*-linux-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.tar.gz" ]; then
        echo "Compressing $file..."
        tar -czf "${file}.tar.gz" "$file"
        echo "Created ${file}.tar.gz"
    elif [ -f "${file}.tar.gz" ]; then
        echo "Skipping $file - tar.gz file already exists"
    fi
done

for file in kanon-*-mac-arm64; do
    if [ -f "$file" ] && [ ! -f "${file}.tar.gz" ]; then
        echo "Compressing $file..."
        tar -czf "${file}.tar.gz" "$file"
        echo "Created ${file}.tar.gz"
    elif [ -f "${file}.tar.gz" ]; then
        echo "Skipping $file - tar.gz file already exists"
    fi
done

echo "tar.gz compression completed!"