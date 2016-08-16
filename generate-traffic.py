#!/usr/bin/python3

import sys
import random
import time
import math
import http.client

minDelay = 0.010
gateway = "localhost:8099"
m1ItemsPath = "/gateway/m1/items"
m2ItemsPath = "/gateway/m2/items"

# =====================
# Post color events
# =====================
def feed(count):
    for e in range(0,count):
        getResource(gateway, m1ItemsPath, "m1-" + str(e))
        time.sleep(minDelay + (0.001 * random.randint(0,100)))
        getResource(gateway, m2ItemsPath, "m2-" + str(e))
        time.sleep(minDelay + (0.001 * random.randint(0,100)))
        if (e % 7 == 0):
            getResource(gateway, m1ItemsPath, "x%20y")
            getResource(gateway, m2ItemsPath, "z%20t")

# =====================
# Post one color event
# =====================
def getResource(host,path,resource):
    c = http.client.HTTPConnection(host)
    c.request("GET",path + "/" + resource)
    response = c.getresponse();
    print(response.read())

# =====================
# Print args
# =====================
for i in range(0, len(sys.argv)):
    print ('Arg#', i, sys.argv[i])

if (len(sys.argv) >= 2):
    random.seed()
    feed(int(sys.argv[1]))
else:
    print ('Syntax error. Number of events is required.')
    print (sys.argv[0], '<EventCount>')
