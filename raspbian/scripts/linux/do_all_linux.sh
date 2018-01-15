#!/bin/sh

set -x
set -e

IMAGE_NAME_NO_EXT=${1}
HOST_NAME=${2}
SSID=${3}
SSID_PWD=${4}
PUBLIC_AUTH_KEY=${5}

if [ "${IMAGE_NAME_NO_EXT}" = "" ] || [ "${HOST_NAME}" = "" ] || [ "${SSID}" = "" || [ "${SSID_PWD}" = "" ] || [ "${PUBLIC_AUTH_KEY}" = "" ]; then
  echo "Usage: ${0} <image name without extension> <host name> <wifi ssid> <wifi pwd> <public ssh key>"
  exit 1
fi

./00_mount_img.sh "/vagrant/images/${IMAGE_NAME_NO_EXT}.img" 'Linux' 'raspbian_image'
./00_mount_img.sh "/vagrant/images/${IMAGE_NAME_NO_EXT}.img" 'W95' 'raspbian_image'
sh -c "./01_initial_config.sh '${HOST_NAME}' '${SSID}' '${SSID_PWD}' '${PUBLIC_AUTH_KEY}'"
./02_umount_img.sh "raspbian_image" "Linux"
./02_umount_img.sh "raspbian_image" "W95"
