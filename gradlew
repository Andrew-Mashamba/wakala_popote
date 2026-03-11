#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0.
#

# Resolve links
app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld "$app_path")
  link=${ls#*' -> '}
  case $link in /*) app_path=$link ;; *) app_path=$(dirname "$app_path")/$link ;; esac
done
APP_HOME=$(cd "${app_path%/*}" >/dev/null && pwd -P) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Java
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  JAVACMD=java
fi

exec "$JAVACMD" -Dfile.encoding=UTF-8 -Xmx64m -Xms64m \
  -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
