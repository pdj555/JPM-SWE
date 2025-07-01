#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.9.6
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != "true" ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVACMD and JAVACCMD
set_java_home() {
  # For Cygwin and MinGW, ensure paths are in Unix format before anything is touched
  if [ -n "${JAVA_HOME-}" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
      JAVACCMD="$JAVA_HOME/jre/sh/javac"
    else
      JAVACMD="$JAVA_HOME/bin/java"
      JAVACCMD="$JAVA_HOME/bin/javac"

      if [ ! -x "$JAVACMD" ] || [ ! -x "$JAVACCMD" ] ; then
        echo "The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run." >&2
        echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" or \"\$JAVA_HOME/bin/javac\" does not exist." >&2
        return 1
      fi
    fi
  else
    JAVACMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v java
    )" || :
    JAVACCMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v javac
    )" || :

    if [ ! -x "${JAVACMD-}" ] || [ ! -x "${JAVACCMD-}" ] ; then
      echo "The java/javac command does not exist in PATH nor is JAVA_HOME set, so mvnw cannot run." >&2
      return 1
    fi
  fi
}

# hash string like Java String::hashCode
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(( ( 31 * h + $(LC_CTYPE=C printf %d "'$char") ) % 4294967296 ))
    str="${str#?}"
  done
  printf %x\\n $h
}

verbose() { :; }
[ "${MVNW_VERBOSE-}" != "true" ] || verbose() { printf %s\\n "${1-}"; }

die() {
  printf %s\\n "$1" >&2
  exit 1
}

# parse distributionUrl and optional distributionSha256Sum, requires .mvn/wrapper/maven-wrapper.properties
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl="${value-}" ;;
  distributionSha256Sum) distributionSha256Sum="${value-}" ;;
  esac
done < "${0%/*}/.mvn/wrapper/maven-wrapper.properties" || die "Cannot read ${0%/*}/.mvn/wrapper/maven-wrapper.properties"

[ -n "${distributionUrl-}" ] || die "distributionUrl is not set in ${0%/*}/.mvn/wrapper/maven-wrapper.properties"

case "${distributionUrl##*/}" in
maven-mvnd-*bin.*) mvnd=true ;;
*) mvnd=false ;;
esac

# apply default values
[ -n "${M2_HOME-}" ] || M2_HOME="${HOME}/.m2"
[ -n "${MAVEN_USER_HOME-}" ] || MAVEN_USER_HOME="${M2_HOME}"
[ -n "${MAVEN_OPTS-}" ] || MAVEN_OPTS=""
[ -n "${MAVEN_ARGS-}" ] || MAVEN_ARGS=""

outputFile="${MAVEN_USER_HOME}/wrapper/maven-wrapper.jar"
if [ -f "$outputFile" ]; then
  verbose "Found $outputFile"
else
  verbose "Couldn't find $outputFile, downloading it ..."
  outputDir="${outputFile%/*}"
  [ -d "$outputDir" ] || mkdir -p "$outputDir"

  # Download maven-wrapper.jar
  case "$(uname)" in
  Darwin*)
    if command -v curl > /dev/null; then
      verbose " - Downloading to: $outputFile"
      curl -fsSL "$distributionUrl" -o "$outputFile"
    else
      verbose " - Downloading to: $outputFile"
      wget -O "$outputFile" "$distributionUrl"
    fi
    ;;
  *)
    if [ -x /usr/bin/wget ]; then
      verbose " - Downloading to: $outputFile"
      /usr/bin/wget -O "$outputFile" "$distributionUrl"
    elif command -v curl > /dev/null; then
      verbose " - Downloading to: $outputFile"
      curl -fsSL "$distributionUrl" -o "$outputFile"
    else
      die "Cannot download $distributionUrl"
    fi
    ;;
  esac

  # If specified, validate the SHA-256 sum of the Maven wrapper jar file
  if [ -n "${distributionSha256Sum-}" ]; then
    verbose "Validating SHA-256 sum of the Maven wrapper jar file"
    case "$(uname)" in
    Darwin)
      if command -v shasum > /dev/null; then
        if echo "$distributionSha256Sum  $outputFile" | shasum -a 256 -c > /dev/null 2>&1; then
          verbose " - Checksum validation successful"
        else
          die "Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised."
        fi
      else
        die "Error: Failed to validate Maven wrapper SHA-256, missing 'shasum' command."
      fi
      ;;
    *)
      if command -v sha256sum > /dev/null; then
        if echo "$distributionSha256Sum  $outputFile" | sha256sum -c > /dev/null 2>&1; then
          verbose " - Checksum validation successful"
        else
          die "Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised."
        fi
      elif command -v shasum > /dev/null; then
        if echo "$distributionSha256Sum  $outputFile" | shasum -a 256 -c > /dev/null 2>&1; then
          verbose " - Checksum validation successful"
        else
          die "Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised."
        fi
      else
        die "Error: Failed to validate Maven wrapper SHA-256, missing 'sha256sum' or 'shasum' command."
      fi
      ;;
    esac
  fi
fi

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
MAVEN_CMD_LINE_ARGS="$MAVEN_ARGS $*"
export MAVEN_CMD_LINE_ARGS

set_java_home

case "$mvnd" in
true)
  exec "$JAVACMD" $MAVEN_OPTS -classpath "$outputFile" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
  ;;
false)
  exec "$JAVACMD" $MAVEN_OPTS -classpath "$outputFile" -Dmaven.home="${M2_HOME}" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
  ;;
esac 