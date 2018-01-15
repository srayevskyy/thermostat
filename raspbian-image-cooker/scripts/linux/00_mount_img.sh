#!/bin/sh

set -x

IMG_FILE=$1
FS_TYPE=$2
MNT_DIR=$3

unset mount_options

if [ "$FS_TYPE" = "W95" ]; then
  mount_options="-t vfat"
elif [ "$FS_TYPE" = "Linux" ]; then
  mount_options="-t ext4"
fi

echo "mount_options: ${mount_options}"

SECTOR_OFFSET=$(sudo /sbin/fdisk -l ${IMG_FILE} | awk -v FS_TYPE="${FS_TYPE}" '$7 == FS_TYPE { print $2 }')
BYTE_OFFSET=$(expr 512 \* $SECTOR_OFFSET)

echo "BYTE_OFFSET=${BYTE_OFFSET}"

IMG_DIR=${MNT_DIR}_${FS_TYPE}

echo Mounting at /mnt/${IMG_DIR}

sudo mkdir -p /mnt/$IMG_DIR
sudo mount ${mount_options} -o loop,offset=$BYTE_OFFSET $IMG_FILE /mnt/$IMG_DIR

