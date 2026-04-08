#!/usr/bin/env fish

set -x TERM xterm-256color

clear

set_color --bold --background cyan black
echo "  clean shadowJar ...  "
set_color normal
echo ""

cd (dirname (status -f))/..
./gradlew --parallel clean desktop:shadowJar