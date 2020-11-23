#!/usr/bin/env bash

U=$1
P=$2
H=${3:-http://api.admin.bootifulpodcast.fm}
E=0
MESSAGE="Please specify a "

if [  "$U" = ""  ]; then
  echo $MESSAGE username
  E=1
fi

if [ "$P" = "" ]; then
  echo $MESSAGE password
  E=1
fi

echo "Authenticating with '$U' / '$P'"

if [[ $E == 1  ]]; then
  echo "Usage: $0 <USERNAME> <PASSWORD>"
  exit 1;
 fi

HOST=$H
curl -H"Authorization: bearer $(curl -XPOST -u jlong:pw ${HOST}/token  > /dev/null 2>&1 )" -XDELETE ${HOST}/admin/caches  > /dev/null 2>&1

