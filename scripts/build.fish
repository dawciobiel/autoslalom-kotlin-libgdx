#!/usr/bin/env fish

set -x TERM xterm-256color

clear

set_color --bold --background yellow black
echo "  clean BUILD ...  "
set_color normal
echo ""

cd (dirname (status -f))/..
./gradlew --parallel clean desktop:build