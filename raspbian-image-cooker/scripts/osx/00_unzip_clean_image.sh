#!/bin/sh

set -e

BOOTSTRAP_ROOT=${1}
IMAGE_NAME_NO_EXT=${2}

rm -fv ${HOME}/${BOOTSTRAP_ROOT}/images/${IMAGE_NAME_NO_EXT}.img

tar xzvf  ${BOOTSTRAP_ROOT}/${IMAGE_NAME_NO_EXT}.zip -C ${BOOTSTRAP_ROOT}/images/
