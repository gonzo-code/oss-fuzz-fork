FROM gcr.io/oss-fuzz-base/base-builder-jvm

RUN curl -L https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip -o maven.zip && \
    unzip maven.zip -d $SRC/maven && \
    rm -rf maven.zip

ENV MVN $SRC/maven/apache-maven-3.6.3/bin/mvn

RUN git clone --depth 1 https://github.com/google/fuzzing && \
    cp fuzzing/dictionaries/html.dict $SRC/HtmlFuzzer.dict && \
    cp fuzzing/dictionaries/xml.dict $SRC/XmlFuzzer.dict && \
    cp fuzzing/dictionaries/xml.dict $SRC/OGXmlFuzzer.dict && \
    rm -rf fuzzing

RUN git clone --depth 1 https://github.com/dvyukov/go-fuzz-corpus && \
    zip -j $SRC/OGXmlFuzzer_seed_corpus.zip go-fuzz-corpus/xml/corpus/* && \
    zip -j $SRC/XmlFuzzer_seed_corpus.zip go-fuzz-corpus/xml/corpus/* && \
    rm -rf go-fuzz-corpus

RUN git clone --depth 1 https://github.com/jhy/jsoup/

COPY build.sh $SRC/


COPY HtmlFuzzer.java DataUtilFuzzer.java ParseFuzzer.java OGXmlFuzzer.java XmlFuzzer.java $SRC/
COPY seeds/datautil_corpus $SRC/datautil_corpus
COPY dicts/encodings.dict $SRC/encodings.dict


WORKDIR $SRC/jsoup
