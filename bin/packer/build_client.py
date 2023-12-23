import os
import sys
import shutil
import subprocess

packer_cmd = sys.argv[1]
subprocess.run([packer_cmd])

out_file = sys.argv[2]
shutil.make_archive(out_file.replace('.zip', ''), 'zip', 'build/packer/client')