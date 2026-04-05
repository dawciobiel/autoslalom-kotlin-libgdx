#!/usr/bin/env fish

set -x TERM xterm-256color

clear

set_color --bold --background yellow black
echo "  BUILDING...  "
set_color normal
echo ""

./gradlew desktop:build