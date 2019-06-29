#!/usr/local/bin/python2.7

import sys
import random
import time
import httplib

minDelay = 0.010
m1ItemsPath = "/gateway/m1/items"
m2ItemsPath = "/gateway/m2/items"


# =====================
# Generate Traffic
# =====================
def feed(count, gateway):
    for e in range(0, count):
        get_resource(gateway, m1ItemsPath, "m1-" + str(e))
        time.sleep(minDelay + (0.001 * random.randint(0,100)))
        get_resource(gateway, m2ItemsPath, "m2-" + str(e))
        time.sleep(minDelay + (0.001 * random.randint(0,100)))
        if e % 7 == 0:
            get_resource(gateway, m1ItemsPath, "x%20y")
            get_resource(gateway, m2ItemsPath, "z%20t")


# =====================
# Get Resource
# =====================
def get_resource(host, path, resource):
    c = httplib.HTTPConnection(host)
    c.request("GET", path + "/" + resource)
    response = c.getresponse()
    print(response.read())


# =====================
# Main
# =====================
for i in range(0, len(sys.argv)):
    print ('Arg#', i, sys.argv[i])

argc = len(sys.argv)
if argc >= 2:
    random.seed()
    gateway = "localhost:8099"
    if argc >= 3 and "-docker"==sys.argv[2]:
        gateway = "localhost"
    feed(int(sys.argv[1]), gateway)
else:
    print ('Syntax error. Number of events is required.')
    print (sys.argv[0], '<Count> [-docker]')
