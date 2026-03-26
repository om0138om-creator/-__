#!/bin/sh
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
exec "$GRADLE_HOME/wrapper/gradle-wrapper.jar" "$@"
