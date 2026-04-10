#!/usr/bin/env fish

set -x TERM xterm-256color

clear

function header
    set_color --bold --background green black
    echo "  [CLEAN]->[BUILD]->[RUN] "
    set_color normal
    echo ""
end

function task_start
    set_color --bold yellow
    echo "$argv "
    set_color normal
    echo ""
end

function task_success
    set_color green
    echo "✓ $argv completed"
    set_color normal
    echo ""
end

function task_error
    set_color --bold red
    echo "✗ $argv failed (exit code: $exit_code)"
    set_color normal
    exit 1
end

function run_gradle
    set -l task_name $argv[1]
    set -l gradle_args $argv[2..-1]

    task_start $task_name
    ./gradlew $gradle_args
    set -l exit_code $status
    if test $exit_code -eq 0
        task_success $task_name
    else
        task_error $task_name
    end
end

function footer
    set_color --bold --background green black
    echo "  ALL TASKS COMPLETED SUCCESSFULLY "
    set_color normal
end


# Header
header

# Go to project root directory
cd (dirname (status -f))/..

# Clean
run_gradle "core:clean" --parallel core:clean
run_gradle "desktop:clean" --parallel desktop:clean

# Build
run_gradle "core:build" --parallel core:build
run_gradle "desktop:build" --parallel desktop:build

# Run
run_gradle "desktop:run" desktop:run

# Footer
footer