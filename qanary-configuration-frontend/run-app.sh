#!/bin/bash

# set -o nounset \
 #    -o errexit \
   #  -o verbose \
    # -o xtrace

# override environment variables if provided as argument
if [ $# -ne 0 ]; then
    echo "-> overriding env with args ..."
    for var in "$@"
    do
        export "$var"
    done
fi

echo "-> ENV vars:"
env | sort

echo "-> User:"
id

echo "-> building app ..."
npm run build --production

echo "=> running app ..."
exec serve -s build
