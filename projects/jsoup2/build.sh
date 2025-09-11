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
################################################################################

# Copy dictionary for the DataUtil fuzzer.
cp $SRC/encodings.dict $OUT/encodings.dict

# Prepare resources for the DataUtil fuzzer.
cp -r $SRC/datautil_corpus $OUT/datautil_fuzzer_seed_corpus
if ls "$OUT/datautil_fuzzer_seed_corpus"/*.b64 1> /dev/null 2>&1; then
  for b64 in "$OUT/datautil_fuzzer_seed_corpus"/*.b64; do
    base64 -d "$b64" > "${b64%.b64}"
    rm "$b64"
  done
fi
cp $OUT/encodings.dict $OUT/datautil_fuzzer.dict

# Remove central publishing plugin that pulls additional build extensions.
perl -0 -i -pe 's|<plugin>\s*<groupId>org\.sonatype\.central</groupId>.*?</plugin>||s' pom.xml

MAVEN_ARGS="-Dmaven.test.skip=true -Djavac.src.version=15 -Djavac.target.version=15"
$MVN package org.apache.maven.plugins:maven-shade-plugin:3.2.4:shade $MAVEN_ARGS
CURRENT_VERSION=$($MVN org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
 -Dexpression=project.version -q -DforceStdout)
cp "target/jsoup-$CURRENT_VERSION.jar" $OUT/jsoup.jar

# Prepare corpora directories.
mkdir -p $SRC/corpus/html5lib $SRC/corpus/wpt $SRC/corpus/jsoup_upstream $SRC/corpus/sanitizer
mkdir -p $OUT/corpus/tokeniser $OUT/corpus/tree $OUT/corpus/cleaner

# html5lib tokenizer and tree-construction tests.
(
  cd $SRC/corpus/html5lib
  curl -L https://github.com/html5lib/html5lib-tests/archive/$HTML5LIB_SHA.zip -o html5lib.zip
  unzip -q html5lib.zip
  rm html5lib.zip
)
python3 <<'PY'
import os, json, glob
src=os.path.join(os.environ['SRC'],'corpus','html5lib','html5lib-tests-'+os.environ['HTML5LIB_SHA'])
tok_out=os.path.join(os.environ['OUT'],'corpus','tokeniser')
tree_out=os.path.join(os.environ['OUT'],'corpus','tree')

for path in glob.glob(os.path.join(src,'tokenizer','*.test')):
    with open(path, encoding='utf-8') as f:
        data=json.load(f)
    for i,test in enumerate(data.get('tests',[])):
        inp=test.get('input')
        if inp is None:
            continue
        with open(os.path.join(tok_out,f'{os.path.basename(path)}_{i}.html'),'w',encoding='utf-8') as o:
            o.write(inp)

for path in glob.glob(os.path.join(src,'tree-construction','*.dat')):
    with open(path, encoding='utf-8') as f:
        lines=f.read().splitlines()
    sections=[]
    collecting=False
    buf=[]
    for line in lines:
        if line.startswith('#data'):
            collecting=True
            buf=[]
            continue
        if line.startswith('#'):
            if collecting and buf:
                sections.append("\n".join(buf))
            collecting=False
            continue
        if collecting:
            buf.append(line)
    if collecting and buf:
        sections.append("\n".join(buf))
    for i,inp in enumerate(sections):
        if inp.strip():
            with open(os.path.join(tree_out,f'{os.path.basename(path)}_{i}.html'),'w',encoding='utf-8') as o:
                o.write(inp)
PY
rm -rf $SRC/corpus/html5lib

# WPT html parsing tests.
(
  cd $SRC/corpus/wpt
  curl -L https://github.com/web-platform-tests/wpt/archive/$WPT_SHA.zip -o wpt.zip
  unzip -q wpt.zip
  rm wpt.zip
)
find $SRC/corpus/wpt/wpt-$WPT_SHA/html/parsing -name '*.html' -size -100k -exec cp {} $OUT/corpus/tree/ \;
find $SRC/corpus/wpt/wpt-$WPT_SHA/html/syntax -name '*.html' -size -100k -exec cp {} $OUT/corpus/tokeniser/ \;
rm -rf $SRC/corpus/wpt

# jsoup upstream tests.
find src/test/resources -name '*.html' -exec cp {} $OUT/corpus/tree/ \;
python3 <<'PY'
import os, glob, re
out_dir=os.path.join(os.environ['OUT'],'corpus','tree')
idx=0
for path in glob.glob('src/test/java/**/*.java', recursive=True):
    try:
        text=open(path,encoding='utf-8').read()
    except Exception:
        continue
    for m in re.finditer(r'"([^"\\]*?(?:\\.[^"\\]*?)*<[^>]+>[^"\\]*?)"', text):
        snippet=bytes(m.group(1),'utf-8').decode('unicode_escape')
        if snippet.strip():
            with open(os.path.join(out_dir,f'jsoup-snippet-{idx}.html'),'w',encoding='utf-8') as f:
                f.write(snippet)
            idx+=1
PY

# Sanitizer corpus.
python3 <<'PY'
import os
out_dir=os.path.join(os.environ['OUT'],'corpus','cleaner')
templates=[
    "<svg onload=alert({n})></svg>",
    "<img src=x onerror=alert({n})>",
    "<a href='javascript:alert({n})'>link</a>",
    "<div onclick='alert({n})'></div>",
    "<img src='data:text/html;base64,PGgxPkhlbGxvPC9oMT4='>",
    "<math href='javascript:alert({n})'></math>",
    "<span style=\"background:url(javascript:alert({n}))\"></span>",
    "<object data='javascript:alert({n})'></object>",
    "<iframe srcdoc='<svg onload=alert({n})>'></iframe>",
    "<p style='width:expression(alert({n}))'></p>"
]
for i in range(50):
    html=templates[i % len(templates)].format(n=i)
    with open(os.path.join(out_dir,f'sanitizer-{i}.html'),'w',encoding='utf-8') as f:
        f.write(html)
PY

# Include sanitizer samples in the other corpora.
cp $OUT/corpus/cleaner/*.html $OUT/corpus/tokeniser/ || true
cp $OUT/corpus/cleaner/*.html $OUT/corpus/tree/ || true

# html dictionary.
cat <<'EOF' > $OUT/html.dict
"<!DOCTYPE html>"
"<!doctype html>"
"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">"
"<script>"
"</script>"
"<style>"
"</style>"
"&amp;"
"&lt;"
"&gt;"
"&quot;"
"onload"
"onerror"
"onclick"
"src"
"href"
"data:"
"javascript:"
EOF

# Zip corpora and cleanup.
(cd $OUT/corpus/tokeniser && find . -type f -name '*.html' | xargs zip -q -9 $OUT/HtmlFuzzer_seed_corpus.zip)
(cd $OUT/corpus/tree && find . -type f -name '*.html' | xargs zip -q -9 $OUT/ParseFuzzer_seed_corpus.zip)
rm -rf $OUT/corpus

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
  if [[ "$fuzzer_basename" == "OGXmlFuzzer" ]]; then
    extra_args="-focus_function=org.jsoup.parser.XmlTreeBuilder.*"
  fi
  if [[ "$fuzzer_basename" == "HtmlFuzzer" || \
        "$fuzzer_basename" == "ParseFuzzer" ]]; then
    extra_args="$extra_args -dict=\$this_dir/html.dict"
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
    extra_args="-dict=\$this_dir/${wrapper_name}.dict -focus_function=org.jsoup.internal.StringUtil.borrowBuilder"
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
  LD_LIBRARY_PATH=\"$JVM_LD_LIBRARY_PATH\":\$this_dir \\
  \$this_dir/jazzer_driver --agent_path=\$this_dir/jazzer_agent_deploy.jar \\
  --cp=$RUNTIME_CLASSPATH \\
  --target_class=${target_class_flag} \\
  --jvm_args=\"\$mem_settings\" \\
  $extra_args \\
  \$@" > $OUT/$wrapper_name
  chmod u+x $OUT/$wrapper_name
done

