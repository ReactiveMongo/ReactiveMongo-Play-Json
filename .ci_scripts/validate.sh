#! /usr/bin/env bash

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

cd "$SCRIPT_DIR/.."

if [[ ! "v$SCALA_VERSION" = "v2.11*" ]]; then
  sbt ++$SCALA_VERSION ';scalafixAll -check ;scalafmtAll'

  git diff --exit-code || (
    echo "ERROR: Scalafmt check failed, see differences above."
    echo "To fix, format your sources using ./build scalafmtAll before submitting a pull request."
    echo "Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request."
    false
  )
fi

TEST_OPTS="exclude mongo2"

TEST_CMD=";error ;test:compile ;mimaReportBinaryIssues"

TEST_CMD="$TEST_CMD ;info ;testQuick * -- $TEST_OPTS"

sbt ++$SCALA_VERSION "$TEST_CMD"
