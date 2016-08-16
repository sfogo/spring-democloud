#!/bin/bash

# =================
# Globals
# =================
STARTED_TAG="Started Application in"
tmpDir="/tmp/democloud"

# ==================
# Wait until started
# ==================
waitUntilStartedTag() {
    tail $1 -n0 -F | while read line; do
        if [[ "$line" =~ "${STARTED_TAG}" ]]; then
            pkill -9 -P $$ tail
        fi
    done
}

# ==========================
# Do not echo anything else!
# ==========================
springBootRun() {
    local app=${1}
    cd ${app}
    mvn spring-boot:run > ${tmpDir}/${app}.pid.${BASHPID}.txt 2>&1 &
    echo $! 
    cd ..
}

# =================
# Start Application
# =================
startApplication() {
    app=${1}
    echo "Starting ${app}..."
    pid=`springBootRun ${app}`
    echo "${pid} " >> ${tmpDir}/pids.txt
    logFile="${tmpDir}/${app}.pid.${pid}.txt"
    waitUntilStartedTag "${logFile}"
    echo "${app} started PID:${pid} Log:${logFile}"
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
