#!/bin/sh

set -e
set -x

IMAGE_NAME_NO_EXT=${1}
DISK_NUMBER=${2}
HOST_NAME=${3}
SSID=${4}
SSID_PWD=${5}
PUBLIC_AUTH_KEY=${6}

BOOTSTRAP_ROOT="$(dirname "${0}")/../.."

if [ "${IMAGE_NAME_NO_EXT}" = "" ] || [ "${DISK_NUMBER}" = "" ] || [ "${HOST_NAME}" = "" ] || [ "${SSID}" = "" ] || [ "${SSID_PWD}" = "" ] || [ "${PUBLIC_AUTH_KEY}" = "" ]; then
  echo "Usage: ${0} <image_name_without extension> <osx disk number from 'diskutil list'> <raspberry host name> <wifi ssid> <wifi passwd> <ssh public key>"
  echo "e.g. ${0} '2017-11-29-raspbian-stretch-lite' '3' 'pizero-device-2' 'wifi_ssid' 'wifi_pwd' 'public auth key'"
  exit 1
fi

vagrant destroy -f || true
./00_unzip_clean_image.sh "${BOOTSTRAP_ROOT}" "${IMAGE_NAME_NO_EXT}"
vagrant up
vagrant ssh -c "cd /vagrant/scripts/linux && ./do_all_linux.sh '${IMAGE_NAME_NO_EXT}' '${HOST_NAME}' '${SSID}' '${SSID_PWD}' '${PUBLIC_AUTH_KEY}'"
./01_upload_image_to_sd.sh "${BOOTSTRAP_ROOT}" "${IMAGE_NAME_NO_EXT}" "${DISK_NUMBER}"
vagrant destroy -f

# sample command to ssh into raspberry
# ssh-keygen -R pizero-device-2 && ssh -o "StrictHostKeyChecking=no" pi@pizero-device-2
