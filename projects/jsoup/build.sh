#!/bin/bash -eu
# Copyright 2024 Google LLC
# Licensed under the Apache License, Version 2.0.
################################################################################

# Move dictionary to $OUT if present.
find $SRC -maxdepth 1 -name "*.dict" -exec mv {} $OUT \;

# Remove central publishing plugin that pulls additional build extensions.
perl -0 -i -pe 's|<plugin>\s*<groupId>org\.sonatype\.central</groupId>.*?</plugin>||s' pom.xml

MAVEN_ARGS="-Dmaven.test.skip=true -Djavac.src.version=15 -Djavac.target.version=15"
$MVN package org.apache.maven.plugins:maven-shade-plugin:3.2.4:shade $MAVEN_ARGS
CURRENT_VERSION=$($MVN org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
 -Dexpression=project.version -q -DforceStdout)
cp "target/jsoup-$CURRENT_VERSION.jar" $OUT/jsoup.jar

ALL_JARS="jsoup.jar"
BUILD_CLASSPATH=$(echo $ALL_JARS | xargs printf -- "$OUT/%s:"):$JAZZER_API_PATH
if [[ -n ${FUZZ_INTROSPECTOR:-} ]]; then
  fi_jar=$(find /fuzz-introspector -name 'fuzz-introspector-jvm-*.jar' -print -quit)
  BUILD_CLASSPATH="$BUILD_CLASSPATH:$fi_jar"
fi

for fuzzer in FuzzParseHtml FuzzParseFragment FuzzChunkedStream; do
  javac -cp $BUILD_CLASSPATH $SRC/$fuzzer.java
  cp $SRC/$fuzzer.class $OUT/
  echo "#!/bin/bash
this_dir=\$(dirname \"\$0\")
cp=\$this_dir:\$(ls \$this_dir/*.jar | tr '\n' ':')
LD_LIBRARY_PATH=\"$JVM_LD_LIBRARY_PATH\":\$this_dir \\
\$this_dir/jazzer_driver \\
--target_class=$fuzzer \\
--cp=\$cp \\
--instrumentation_excludes=com.sun.**,java.**,javax.** \\
--jvm_args=\"-Xss256m -Xmx1024m -Dfile.encoding=UTF-8\" \\
--dict=\$this_dir/jsoup_html.dict \\
\$@ \\
-timeout=3 -use_value_profile=1 -entropic=1 -max_len=4096 -rss_limit_mb=4096" > $OUT/$fuzzer
  chmod +x $OUT/$fuzzer
done

# Seed corpora
mkdir -p $OUT/FuzzParseHtml_seed_corpus
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed1.html
<b><i></b></i>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed2.html
<table><table><tr><td></td></tr></table></table>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed3.html
<svg><script></script><circle/></svg>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed4.html
<math><mi>x</mi></math>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed5.html
<a href='<<<<>>>>'''''>
SEED
printf "<a title='\x00\x00'>x" > $OUT/FuzzParseHtml_seed_corpus/seed6.html
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed7.html
<div />text
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed8.html
</td></tr>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed9.html
<li><li>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed10.html
<button><button>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed11.html
<div><p></div>
SEED
cat <<'SEED' > $OUT/FuzzParseHtml_seed_corpus/seed12.html
<a href="&lt;&gt;&amp;"></a>
SEED
zip -j -q $OUT/FuzzParseHtml_seed_corpus.zip $OUT/FuzzParseHtml_seed_corpus/*

mkdir -p $OUT/FuzzParseFragment_seed_corpus
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed1.html
</td></tr>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed2.html
<li><li>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed3.html
<button><button>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed4.html
<svg><script></script></svg>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed5.html
<math><mi>x</mi>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed6.html
<a href='<<<<>>>>'''''>
SEED
printf "<a title='\x00\x00'>x" > $OUT/FuzzParseFragment_seed_corpus/seed7.html
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed8.html
<div />text
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed9.html
<b><i></b></i>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed10.html
<table><tr><td>
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed11.html
<!--comment-->
SEED
cat <<'SEED' > $OUT/FuzzParseFragment_seed_corpus/seed12.html
<template><td></template>
SEED
zip -j -q $OUT/FuzzParseFragment_seed_corpus.zip $OUT/FuzzParseFragment_seed_corpus/*

mkdir -p $OUT/FuzzChunkedStream_seed_corpus
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed1.html
<svg><script></script>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed2.html
<table><tr><td></td></tr></table>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed3.html
<b><i></b></i>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed4.html
<a href='test'>
SEED
printf "\xE2\x82" > $OUT/FuzzChunkedStream_seed_corpus/seed5.html
printf "\xF0\x90\x80" > $OUT/FuzzChunkedStream_seed_corpus/seed6.html
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed7.html
<div />text
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed8.html
<li><li>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed9.html
<button><button>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed10.html
<math><mi>x</mi></math>
SEED
cat <<'SEED' > $OUT/FuzzChunkedStream_seed_corpus/seed11.html
<a title='zz'>x
SEED
printf "</td>" > $OUT/FuzzChunkedStream_seed_corpus/seed12.html
zip -j -q $OUT/FuzzChunkedStream_seed_corpus.zip $OUT/FuzzChunkedStream_seed_corpus/*
