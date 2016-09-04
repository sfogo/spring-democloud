#!/bin/bash

# =================
# Globals
# =================
tmpDir="/tmp/democloud"
STARTED_TAG="Started Application in"
export SPRING_PROFILES_ACTIVE=native,dev

# ==================
# Wait until started
# ==================
waitUntilStartedTag() {
    touch $1
    tail $1 -n0 -F | while read line; do
        if [[ "$line" =~ "${STARTED_TAG}" ]]; then
            pkill -9 -P $$ tail
        fi
    done
}

# =================
# Start Application
# =================
startApplication() {
    local app=${1}
    cd ${app}

    echo "Starting ${app}..."
    mvn spring-boot:run > ${tmpDir}/${app}.pid.${BASHPID}.txt 2>&1 &
    pid=$!
    echo "${pid} " >> ${tmpDir}/pids.txt

    logFile="${tmpDir}/${app}.pid.${pid}.txt"
    waitUntilStartedTag "${logFile}"
    echo "${app} started PID:${pid} Log:${logFile}"

    cd ..
}

# =================
# Start
# =================
rm -fR ${tmpDir}
mkdir -p ${tmpDir}
touch ${tmpDir}/pids.txt

startApplication config-server
startApplication eureka
startApplication m3-service
startApplication m2-service
startApplication m1-service
startApplication gateway
startApplication turbine
startApplication dashboard

echo Done.
echo "You can shut it all down with : kill \`cat ${tmpDir}/pids.txt\`"
