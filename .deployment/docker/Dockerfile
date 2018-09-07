ARG IMAGE_REPOSITORY=861104244336.dkr.ecr.eu-west-1.amazonaws.com/sysops
ARG IMAGE_NAME=java-jre
ARG IMAGE_TAG=8

FROM $IMAGE_REPOSITORY/$IMAGE_NAME:$IMAGE_TAG

ENV SCALA_VERSION=2.12.4 \
    SCALA_TAR_URL=https://www.scala-lang.org/files/archive \
    SBT_VERSION=0.13.0
    
#install scala and sbt
RUN yum install -y wget && \
    wget -O /scala-$SCALA_VERSION.tar $SCALA_TAR_URL/scala-$SCALA_VERSION.tgz && \
    tar xvf /scala-$SCALA_VERSION.tar && \
    mv /scala-$SCALA_VERSION.tar /usr/lib && \
    ln -s /usr/lib/scala-$SCALA_VERSION /usr/lib/scala && \
    wget http://dl.bintray.com/sbt/rpm/sbt-$SBT_VERSION.rpm && \
    yum install -y sbt-$SBT_VERSION.rpm

ENV PATH $PATH:${SBT_HOME}/bin

COPY . /code

WORKDIR /code

# NOTE: Init sbt
RUN sbt sbtVersion
# NOTE: Separate in two layers to optimize docker build time
RUN sbt assembly

CMD ["java", "-jar", "/code/target/scala-2.12/schema-registry.jar"]
