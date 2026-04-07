#!/usr/bin/env fish

set -x TERM xterm-256color

clear

set_color --bold --background cyan black
echo "  FAT JAR CREATING...  "
set_color normal
echo ""

cd (dirname (status -f))/..
./gradlew clean desktop:shadowJar