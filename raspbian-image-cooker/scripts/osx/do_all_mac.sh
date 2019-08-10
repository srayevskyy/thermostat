#!/bin/sh

set -x
set -e

function unzip_clean_image() {
  BOOTSTRAP_ROOT=${1}
  IMAGE_NAME_NO_EXT=${2}
  rm -fv ${HOME}/${BOOTSTRAP_ROOT}/images/${IMAGE_NAME_NO_EXT}.img
  tar xzvfk ${BOOTSTRAP_ROOT}/${IMAGE_NAME_NO_EXT}.zip -C ${BOOTSTRAP_ROOT}/images/ || true
}

function upload_image_to_sd() {
  BOOTSTRAP_ROOT=${1}
  IMAGE_NAME_NO_EXT=${2}
  DISK_NUMBER=${3}

  diskutil unmountDisk /dev/disk${DISK_NUMBER}
  sudo dd bs=200m if=${BOOTSTRAP_ROOT}/images/${IMAGE_NAME_NO_EXT}.img of=/dev/rdisk${DISK_NUMBER} conv=sync
  sudo diskutil eject /dev/rdisk${DISK_NUMBER}
}

if [ "${IMAGE_NAME_NO_EXT}" = "" ] || [ "${DISK_NUMBER}" = "" ] || [ "${HOST_NAME}" = "" ] || [ "${SSID}" = "" ] || [ "${SSID_PWD}" = "" ] || [ "${PUBLIC_AUTH_KEY}" = "" ]; then
  echo "Usage: ${0} <image_name_without extension> <osx disk number from 'diskutil list'> <raspberry host name> <wifi ssid> <wifi passwd> <ssh public key>"
  echo "e.g. ${0} '2017-11-29-raspbian-stretch-lite' '3' 'pizero-device-2' 'wifi_ssid' 'wifi_pwd' 'public auth key'"
  exit 1
fi

BOOTSTRAP_ROOT="$(dirname "${0}")/../.."

#vagrant destroy -f || true
#vagrant up
unzip_clean_image "${BOOTSTRAP_ROOT}" "${IMAGE_NAME_NO_EXT}"
vagrant ssh -c "cd /vagrant/scripts/linux && bash ./do_all_linux.sh ${IMAGE_NAME_NO_EXT} ${HOST_NAME} ${SSID} ${SSID_PWD} '${PUBLIC_AUTH_KEY}'"
#upload_image_to_sd "${BOOTSTRAP_ROOT}" "${IMAGE_NAME_NO_EXT}" "${DISK_NUMBER}"
#vagrant destroy -f
#ssh-keygen -R ${HOST_NAME}

# sample command to ssh into raspberry
# ssh-keygen -R ${HOST_NAME} && ssh -o "StrictHostKeyChecking=no" pi@pizero-name
