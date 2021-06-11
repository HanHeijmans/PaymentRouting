import os
import subprocess
import time

prc = []
threads = 20

for n in range(threads):
    prc.append(subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(n), 'true']))

# count = threads
# runs = 20
# while True:
#     a = False
#     for n, p in enumerate(prc):
#         if p.poll() != 0 or p.poll() != -1:
#             a = True
#         elif count < runs:
#             prc[n] = subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(count), 'true'])
#             count += 1
#     if not a and count >= runs:
#         subprocess.Popen(['java', '-jar', 'PaymentRouting.jar', str(count), 'true'])
#         print('Done')
#         break;
#     time.sleep(1)



    