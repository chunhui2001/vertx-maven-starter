#!/usr/bin/env bash

export LAUNCHER="io.vertx.core.Launcher"
export VERTICLE="com.shenmao.vertx.starter.MainVerticle"
export CMD="mvn compile"
export VERTX_CMD="run"

# java -cp $(echo target/dependency/*.jar | tr ' ' ':'):"target/classes" io.vertx.core.Launcher run com.shenmao.vertx.starter.MainVerticle

mvn compile dependency:copy-dependencies
java \
  -cp  $(echo target/dependency/*.jar | tr ' ' ':'):"target/classes" \
  $LAUNCHER $VERTX_CMD $VERTICLE \
  --redeploy="src/main/**/*" --on-redeploy="$CMD" \
  --launcher-class=$LAUNCHER \
  $@
