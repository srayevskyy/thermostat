#!/bin/sh

set -e
set -x

BOOTSTRAP_ROOT=${1}
IMAGE_NAME_NO_EXT=${2}
DISK_NUMBER=${3}

diskutil unmountDisk /dev/disk${DISK_NUMBER}
sudo dd bs=200m if=${BOOTSTRAP_ROOT}/images/${IMAGE_NAME_NO_EXT}.img of=/dev/rdisk${DISK_NUMBER} conv=sync
sudo diskutil eject /dev/rdisk${DISK_NUMBER}
