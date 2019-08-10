#!/bin/sh

set -x
set -e

MOUNTDIR=""

mount_img() {
  IMG_FILE=$1
  FS_TYPE=$2
  MNT_DIR=$3

  unset mount_options

  if [ "$FS_TYPE" = "fat32" ]; then
    mount_options="-t vfat"
  elif [ "$FS_TYPE" = "ext4" ]; then
    mount_options="-t ext4"
  fi

  echo "mount_options: ${mount_options}"

  BYTE_OFFSET=$( parted -s ${IMG_FILE} unit b print | grep "${FS_TYPE}" | awk '{print $2}' | sed 's/B//' )

  echo "BYTE_OFFSET=${BYTE_OFFSET}"

  IMG_DIR=${MNT_DIR}_${FS_TYPE}

  MOUNTDIR=/mnt/$IMG_DIR

  echo "Mounting at ${MOUNTDIR}"

  sudo mkdir -p ${MOUNTDIR}

  sudo mount ${mount_options} -o loop,offset=$BYTE_OFFSET $IMG_FILE ${MOUNTDIR}
}

setup_hostname() {
  WORKDIR=${1}
  NEW_HOSTNAME_TO_SET=${2}
  echo ${NEW_HOSTNAME_TO_SET} | sudo tee ${WORKDIR}/etc/hostname
}

setup_wpa_supplicant_on_boot() {

  WORKDIR=$1
  NEW_SSID_TO_SET=$2
  NEW_PWD_TO_SET=$3

  filename=${WORKDIR}/wpa_supplicant.conf

  if ! sudo grep -q "network=" ${filename}; then

  cat << EOF | sudo tee ${filename}

ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
update_config=1
country=US

network={
    ssid="${NEW_SSID_TO_SET}"
    psk="${NEW_PWD_TO_SET}"
    key_mgmt=WPA-PSK
    id_str="AP1"
}

EOF

  else
      echo "${filename}: nothing to do"
  fi

}

enable_ssh_on_boot() {
    WORKDIR=$1
    sudo touch ${WORKDIR}/ssh
}

setup_authorized_keys() {
    WORKDIR=$1
    AUTHORIZED_KEY=$2
    PI_HOME=${WORKDIR}/home/pi
    pi_userid=$(cat ${WORKDIR}/etc/passwd | grep -e "^pi:" | awk -F ':' '{print $3}')
    echo "Pi user ID: ${pi_userid}"
    mkdir -p ${PI_HOME}/.ssh
    echo "${AUTHORIZED_KEY}" > ${PI_HOME}/.ssh/authorized_keys
    chown -R ${pi_userid} ${PI_HOME}/.ssh
    chmod 700 ${PI_HOME}/.ssh && chmod 600 ${PI_HOME}/.ssh/*
}

do_all() {
  IMAGE_NAME_NO_EXT=${1}
  HOST_NAME=${2}
  SSID=${3}
  SSID_PWD=${4}
  PUBLIC_AUTH_KEY=${5}

  # mandatory parameters check
  if [ "${IMAGE_NAME_NO_EXT}" == "" ] || [ "${HOST_NAME}" == "" ] || [ "${SSID}" == "" || [ "${SSID_PWD}" == "" ] || [ "${PUBLIC_AUTH_KEY}" == "" ]; then
    echo "Usage: ${0} <image name without extension> <host name> <wifi ssid> <wifi pwd> <public ssh key>"
    exit 1
  fi

  # copy image locally, because mount from shared folder does not work
  rm -fv /tmp/${IMAGE_NAME_NO_EXT}.img
  cp -n -v /vagrant/images/${IMAGE_NAME_NO_EXT}.img /tmp/

  mount_img "/tmp/${IMAGE_NAME_NO_EXT}.img" 'ext4' 'raspbian_image'
  EXT4_MOUNTDIR=$MOUNTDIR
  setup_hostname "${EXT4_MOUNTDIR}" "${HOST_NAME}"
  setup_authorized_keys "/mnt/raspbian_image_ext4" "${PUBLIC_AUTH_KEY}"
  sudo umount ${EXT4_MOUNTDIR}

  mount_img "/tmp/${IMAGE_NAME_NO_EXT}.img" 'fat32' 'raspbian_image'
  FAT32_MOUNTDIR=$MOUNTDIR
  setup_wpa_supplicant_on_boot "${FAT32_MOUNTDIR}" ${SSID} ${SSID_PWD}
  enable_ssh_on_boot "${FAT32_MOUNTDIR}"
  sudo umount ${FAT32_MOUNTDIR}

  # move image back to shared folder
  cp -v /tmp/${IMAGE_NAME_NO_EXT}.img /vagrant/images/
}

do_all "${1}" "${2}" "${3}" "${4}" "${5}"
