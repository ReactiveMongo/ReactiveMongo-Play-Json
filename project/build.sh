#!/usr/bin/env bash
  
set -e

S2_11="2.11.12"
S2_12="2.12.19"
S2_13="2.13.14"
S3="3.3.6"

unset REACTIVEMONGO_SHADED

TASKS=";makePom ;packageBin ;packageSrc ;packageDoc"

export RELEASE_SUFFIX=play25 PLAY_VERSION=2.5.7
sbt $SBT_OPTS ++${S2_11} clean "$TASKS"

export RELEASE_SUFFIX=play26 PLAY_VERSION=2.6.7
sbt $SBT_OPTS ++${S2_11} "$TASKS" ++${S2_12} "$TASKS"

export RELEASE_SUFFIX=play27 PLAY_VERSION=2.7.4
sbt $SBT_OPTS ++${S2_11} "$TASKS" ++${S2_12} "$TASKS" ++${S2_13} "$TASKS"

export RELEASE_SUFFIX=play28 PLAY_VERSION=2.8.1
sbt $SBT_OPTS ++${S2_12} "$TASKS" ++${S2_13} "$TASKS"

export RELEASE_SUFFIX=play29 PLAY_VERSION=2.9.1
sbt $SBT_OPTS ++${S2_12} "$TASKS" ++${S2_13} "$TASKS"

# Scala3 doc workaround
S3_DOC_VER="3.6.2"
S3_TASKS=";makePom ;packageBin ;packageSrc ;++${S3_DOC_VER}! ;packageDoc"

export RELEASE_SUFFIX=play210 PLAY_VERSION=2.10.4
sbt $SBT_OPTS ++${S2_12} "$TASKS" ++${S2_13} "$TASKS" ++${S3} "${S3_TASKS}"

export RELEASE_SUFFIX=play30 PLAY_VERSION=3.0.2
sbt $SBT_OPTS ++${S2_12} "$TASKS" ++${S2_13} "$TASKS" ++${S3} "${S3_TASKS}"

for F in "compat/target/scala-${S3_DOC_VER}/"reactivemongo*-javadoc.jar; do (
    N="$(basename "$F")"
    M="$(echo "$N" | sed -e "s/${S3_DOC_VER}/${S3}/")"

    echo "$N => $M"
    mv "$F" "compat/target/scala-${S3}/$M"
); done
