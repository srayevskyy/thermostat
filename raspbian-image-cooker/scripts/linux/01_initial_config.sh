#!/bin/bash

set -e
set -x

HOST_NAME=${1}
SSID=${2}
SSID_PWD=${3}
PUBLIC_AUTH_KEY=${4}

function setup_hostname() {
  WORKDIR=${1}
  NEW_HOSTNAME_TO_SET=${2}
  echo ${NEW_HOSTNAME_TO_SET} | sudo tee ${WORKDIR}/etc/hostname
}

function setup_wpa_supplicant_on_boot() {

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
}
EOF

  else
      echo "${filename}: nothing to do"
  fi

}

function enable_ssh_on_boot() {
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

setup_hostname "/mnt/raspbian_image_Linux" "${HOST_NAME}"
setup_wpa_supplicant_on_boot "/mnt/raspbian_image_W95" ${SSID} ${SSID_PWD}
enable_ssh_on_boot "/mnt/raspbian_image_W95"
setup_authorized_keys "/mnt/raspbian_image_Linux" "${PUBLIC_AUTH_KEY}"
