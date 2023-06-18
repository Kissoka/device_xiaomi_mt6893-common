#!/bin/bash
#
# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017-2020 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

set -e

DEVICE=agate
VENDOR=xiaomi

# Load extract_utils and do some sanity checks
MY_DIR="${BASH_SOURCE%/*}"
if [[ ! -d "${MY_DIR}" ]]; then MY_DIR="${PWD}"; fi

ANDROID_ROOT="${MY_DIR}/../../.."

HELPER="${ANDROID_ROOT}/tools/extract-utils/extract_utils.sh"
if [ ! -f "${HELPER}" ]; then
    echo "Unable to find helper script at ${HELPER}"
    exit 1
fi
source "${HELPER}"

function blob_fixup {
    case "$1" in
        system/lib/libsink.so)
            "$PATCHELF" --add-needed "libshim_vtservice.so" "$2"
            ;;
        vendor/lib*/libudf.so)
            "$PATCHELF" --replace-needed "libunwindstack.so" "libunwindstack-v30.so" "$2"
            ;;
        system/lib/libshowlogo.so)
            "$PATCHELF" --add-needed "libshim_showlogo.so" "$2"
            ;;
        vendor/lib*/libmtkcam_stdutils.so)
            "$PATCHELF" --replace-needed "libutils.so" "libutils-v30.so" "$2"
            ;;
        vendor/lib*/hw/audio.primary.mt6893.so)
            "$PATCHELF" --replace-needed "libmedia_helper.so" "libmedia_helper-v30.so" "$2"
            ;;
	vendor/etc/init/init.batterysecret.rc | vendor/etc/init/mi_ic.rc | vendor/etc/init/hw/init.mi_thermald.rc | vendor/etc/init/init.charge_logger.rc | vendor/etc/init/mi_ric.rc)
            sed -i '/seclabel/d' "${2}"
            ;;
    esac
}

# Default to sanitizing the vendor folder before extraction
CLEAN_VENDOR=true

KANG=
SECTION=

while [ "${#}" -gt 0 ]; do
    case "${1}" in
        -n | --no-cleanup )
                CLEAN_VENDOR=false
                ;;
        -k | --kang )
                KANG="--kang"
                ;;
        -s | --section )
                SECTION="${2}"; shift
                CLEAN_VENDOR=false
                ;;
        * )
                SRC="${1}"
                ;;
    esac
    shift
done

if [ -z "${SRC}" ]; then
    SRC="adb"
fi

# Initialize the helper
setup_vendor "${DEVICE}" "${VENDOR}" "${ANDROID_ROOT}" false "${CLEAN_VENDOR}"

extract "${MY_DIR}/proprietary-files.txt" "${SRC}" "${KANG}" --section "${SECTION}"

"${MY_DIR}/setup-makefiles.sh"
