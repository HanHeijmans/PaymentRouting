import os
import subprocess
import time

prc = []
threads = 10

for n in range(threads):
    prc.append(subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(n), 'false']))

count = threads
runs = 10
while True:
    a = False
    for n, p in enumerate(prc):
        if p.poll() is None:
            a = True
        elif count < runs:
            prc[n] = subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(count), 'false'])
            count += 1
    if not a and count >= runs:
        # subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(count - 1), 'true'])
        print('Done')
        break;
    time.sleep(1)



    