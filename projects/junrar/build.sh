#!/bin/bash -eu
set -euxo pipefail
# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################

mv $SRC/*.zip $OUT

./gradlew build
CURRENT_VERSION=$(./gradlew properties --no-daemon --console=plain -q | grep "^version:" | awk '{printf $2}')
cp "build/libs/junrar-$CURRENT_VERSION-sources.jar" $OUT/junrar.jar

curl -L https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar -o $OUT/slf4j-api.jar
curl -L https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar -o $OUT/slf4j-simple.jar

ALL_JARS="junrar.jar slf4j-api.jar slf4j-simple.jar"

# The classpath at build-time includes the project jars in $OUT as well as the
# Jazzer API.
BUILD_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "$OUT/%s:"):$JAZZER_API_PATH

# All .jar and .class files lie in the same directory as the fuzzer at runtime.
RUNTIME_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "\$this_dir/%s:"):\$this_dir

# Copy dict if present
if [[ -f "$SRC/projects/junrar/junrar.dict" ]]; then
  cp "$SRC/projects/junrar/junrar.dict" "$OUT/junrar.dict"
fi

# Create seed corpus if seeds exist (non-fatal if none)
SEED_DIR="$SRC/projects/junrar/seeds"
if [[ -d "$SEED_DIR" ]]; then
  shopt -s nullglob
  SEEDS=("$SEED_DIR"/*)
  if (( ${#SEEDS[@]} )); then
    (cd "$SEED_DIR" && zip -q -r "$OUT/junrar_seed_corpus.zip" .)
  fi
  shopt -u nullglob
fi

# Provide safe default Jazzer flags
export JAZZER_FLAGS="${JAZZER_FLAGS:-} -use_value_profile=1 -keep_going=1 -timeout=60"
if [[ -f "$OUT/junrar.dict" ]]; then
  export JAZZER_FLAGS="$JAZZER_FLAGS -dict=$OUT/junrar.dict"
fi

for fuzzer in $(find $SRC -name '*Fuzzer.java'); do
  fuzzer_basename=$(basename -s .java $fuzzer)
  fuzzer_pkg=$(grep -E '^package ' "$fuzzer" | sed 's/package \(.*\);/\1/')
  if [[ -n "$fuzzer_pkg" ]]; then
    fuzzer_classname="$fuzzer_pkg.$fuzzer_basename"
  else
    fuzzer_classname="$fuzzer_basename"
  fi
  javac -cp $BUILD_CLASSPATH -d $OUT $fuzzer

  # Create an execution wrapper that executes Jazzer with the correct arguments.
  echo "#!/bin/bash
# LLVMFuzzerTestOneInput for fuzzer detection.
this_dir=\$(dirname \"\$0\")
if [[ \"\$@\" =~ (^| )-runs=[0-9]+($| ) ]]; then
  mem_settings='-Xmx1900m:-Xss900k'
else
  mem_settings='-Xmx2048m:-Xss1024k'
fi
LD_LIBRARY_PATH=\"$JVM_LD_LIBRARY_PATH\":\$this_dir \
\$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \
--cp=$RUNTIME_CLASSPATH \
--target_class=$fuzzer_classname \
--jvm_args=\"\$mem_settings\" \
$JAZZER_FLAGS \
\$@" > $OUT/$fuzzer_basename
  chmod u+x $OUT/$fuzzer_basename
done
