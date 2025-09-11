# Copyright 2021 Google LLC
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

FROM gcr.io/oss-fuzz-base/base-builder-jvm

RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    jq \
    git \
    python3 && \
    rm -rf /var/lib/apt/lists/*

RUN curl -L https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip -o maven.zip && \
    unzip maven.zip -d $SRC/maven && \
    rm -rf maven.zip

ENV MVN $SRC/maven/apache-maven-3.6.3/bin/mvn

ENV HTML5LIB_SHA=f994590f528ac8b6073665791ddb1ed85c66dfb2
ENV WPT_SHA=5045ad52dc8584dbaf3ec4c0a7b958eb0b073380

RUN git clone --depth 1 https://github.com/jhy/jsoup/

COPY build.sh $SRC/


COPY HtmlFuzzer.java DataUtilFuzzer.java ParseFuzzer.java OGXmlFuzzer.java XmlFuzzer.java $SRC/
COPY seeds/datautil_corpus $SRC/datautil_corpus
COPY dicts/encodings.dict $SRC/encodings.dict


WORKDIR $SRC/jsoup
