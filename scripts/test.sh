#!/bin/bash
# Chart A11y Plugin Test Script

set -e

echo "========================================="
echo "Chart A11y Plugin Test Suite"
echo "========================================="

cd "$(dirname "$0")/.."

echo ""
echo "Step 1: Running plugin unit tests..."
echo "-----------------------------------------"
./gradlew :plugin:test --info

echo ""
echo "Step 2: Publishing plugin to local Maven repo..."
echo "-----------------------------------------"
./gradlew :plugin:publishToMavenLocal
./gradlew :plugin:publish

echo ""
echo "Step 3: Publishing runtime to local Maven repo..."
echo "-----------------------------------------"
./gradlew :runtime:publishToMavenLocal

echo ""
echo "Step 4: Building sample app..."
echo "-----------------------------------------"
./gradlew :sample:assembleDebug

echo ""
echo "========================================="
echo "All tests passed!"
echo "========================================="
