#!/usr/bin/env fish

set -x TERM xterm-256color

clear

set_color --bold --background green black
echo "  clean build RUN ...  "
set_color normal
echo ""

cd (dirname (status -f))/..
./gradlew --parallel clean desktop:build desktop:run