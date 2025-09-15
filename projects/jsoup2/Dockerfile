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


COPY TokeniserFuzzer.java TreeBuilderFuzzer.java CleanerFuzzer.java DataUtilFuzzer.java OGXmlFuzzer.java XmlFuzzer.java $SRC/
COPY seeds/datautil_corpus $SRC/datautil_corpus
COPY dicts/encodings.dict $SRC/encodings.dict


WORKDIR $SRC/jsoup
