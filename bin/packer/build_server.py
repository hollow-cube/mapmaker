import os
import sys
import shutil
import subprocess

packer_cmd = sys.argv[1]
subprocess.run([packer_cmd, "out_here_hack"])

for path in sys.argv[2:]:
    file = path.split("/")[-1]
    shutil.copyfile("server/" + file, path)