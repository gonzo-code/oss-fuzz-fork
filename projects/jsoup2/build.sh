#!/bin/bash -eu
# Copyright 2021 Google Inc.
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

# Move seed corpus and dictionary.
mv $SRC/{*.zip,*.dict} $OUT

# Prepare resources for the DataUtil fuzzer.
cp -r $SRC/datautil_corpus $OUT/datautil_fuzzer_seed_corpus
cp $OUT/encodings.dict $OUT/datautil_fuzzer.dict

# Remove central publishing plugin that pulls additional build extensions.

perl -0 -i -pe 's|<plugin>\s*<groupId>org\.sonatype\.central</groupId>.*?</plugin>||s' pom.xml

MAVEN_ARGS="-Dmaven.test.skip=true -Djavac.src.version=15 -Djavac.target.version=15"
$MVN package org.apache.maven.plugins:maven-shade-plugin:3.2.4:shade $MAVEN_ARGS
CURRENT_VERSION=$($MVN org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
 -Dexpression=project.version -q -DforceStdout)
cp "target/jsoup-$CURRENT_VERSION.jar" $OUT/jsoup.jar

ALL_JARS="jsoup.jar"

# The classpath at build-time includes the project jars in $OUT as well as the
# Jazzer API.
BUILD_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "$OUT/%s:"):$JAZZER_API_PATH

if [[ -n ${FUZZ_INTROSPECTOR:-} ]]; then
  fi_jar=$(find /fuzz-introspector -name 'fuzz-introspector-jvm-*.jar' -print -quit)
  BUILD_CLASSPATH="$BUILD_CLASSPATH:$fi_jar"
fi

# All .jar and .class files lie in the same directory as the fuzzer at runtime.
RUNTIME_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "\$this_dir/%s:"):\$this_dir

for fuzzer in $(find "$SRC" -maxdepth 1 -name '*Fuzzer.java'); do
  fuzzer_basename=$(basename -s .java "$fuzzer")
  extra_args=""
  wrapper_name="$fuzzer_basename"
  if [[ "$fuzzer_basename" == "XmlFuzzer" ]]; then
    extra_args="-focus_function=org.jsoup.parser.XmlTreeBuilder.*"
  fi
  javac -cp $BUILD_CLASSPATH -d $OUT "$fuzzer"
  package_name=$(grep -E '^package ' "$fuzzer" | sed 's/package \(.*\);/\1/')
  if [[ -n "$package_name" ]]; then
    target_class="$package_name.$fuzzer_basename"
  else
    target_class="$fuzzer_basename"
  fi

  target_class_flag=$target_class
  extra_env=""
  if [[ "$fuzzer_basename" == "DataUtilFuzzer" ]]; then
    wrapper_name="datautil_fuzzer"
    extra_env="export FUZZ_TARGET_CLASS=$target_class"
    target_class_flag="\$FUZZ_TARGET_CLASS"
  fi

  # Create an execution wrapper that executes Jazzer with the correct arguments.
  echo "#!/bin/bash
# LLVMFuzzerTestOneInput for fuzzer detection.
this_dir=\$(dirname \"\$0\")
${extra_env}
if [[ \"\$@\" =~ (^| )-runs=[0-9]+($| ) ]]; then
  mem_settings='-Xmx1900m:-Xss900k'
else
  mem_settings='-Xmx2048m:-Xss1024k'
fi
  LD_LIBRARY_PATH=\"$JVM_LD_LIBRARY_PATH\":\$this_dir \
  \$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \
  --cp=$RUNTIME_CLASSPATH \
  --target_class=${target_class_flag} \
  --jvm_args=\"\$mem_settings\" \
  $extra_args \
  \$@" > $OUT/$wrapper_name
  chmod u+x $OUT/$wrapper_name
done

