#!/bin/sh
set -eu

echo "Compiling..."
mvn clean package -DskipTests

echo "Starting app..."
exec java -jar target/*.jar