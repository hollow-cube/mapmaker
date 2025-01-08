#!/bin/bash

find_process() {
    ps aux | grep "modules/core" | grep -v grep
}

kill_process() {
    local pid=$1
    if kill -9 "$pid"; then
        echo "Process $pid has been killed."
    else
        echo "Failed to kill process $pid."
    fi
}

process_list=$(find_process)

if [[ -z "$process_list" ]]; then
    echo "No process found with 'bin/development' in its command."
    exit 0
fi

echo "Matching processes:"
echo "$process_list"

pids=($(echo "$process_list" | awk '{print $2}'))

for pid in "${pids[@]}"; do
    kill_process "$pid"
done
