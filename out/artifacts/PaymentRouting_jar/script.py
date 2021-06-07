import os
import subprocess
import time

prc = []

for n in range(6):
    prc.append(subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(n)]))

count = len(prc)
runs = 10
while True:
    a = False
    for n, p in enumerate(prc):
        if p.poll() != 0:
            a = True
        elif count < runs:
            prc[n] = subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(count)])
            count += 1
    if not a and count >= runs:
        print('Done')
        break;
    time.sleep(1)



    