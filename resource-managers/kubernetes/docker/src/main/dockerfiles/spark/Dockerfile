#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# C3 customization
FROM locked-registry.c3.ai/ubi/ubi8-minimal:8.9.1029.1
#FROM registry.access.redhat.com/ubi8-minimal:8.9-1029
# end C3 customization

# Before building the docker image, first build and make a Spark distribution following
# the instructions in https://spark.apache.org/docs/latest/building-spark.html.
# If this docker file is being used in the context of building your images from a Spark
# distribution, the docker build command should be invoked from the top level directory
# of the Spark distribution. E.g.:
# docker build -t spark:latest -f kubernetes/dockerfiles/spark/Dockerfile .

# C3 customization
# https://catalog.redhat.com/software/containers/ubi8/openjdk-8/5dd6a48dbed8bd164a09589a?gti-tabs=unauthenticated&container-tabs=dockerfile
# microdnf doesn't support installing local rpms, so we have to install dnf in order to install the Corretto RPM with dependencies
# before building, update CORRETTO_VERSION to the version resolved when you call
# wget https://corretto.aws/downloads/latest/amazon-corretto-11-x64-linux-jdk.rpm
ENV CORRETTO_VERSION=11.0.21.9
ENV CORRETTO_RPM=java-11-amazon-corretto-devel-${CORRETTO_VERSION}-1.x86_64.rpm
RUN set -ex && \
    microdnf install --nodocs -y dnf wget && \
    wget https://corretto.aws/downloads/resources/${CORRETTO_VERSION}.1/${CORRETTO_RPM} && \
    dnf install --nodocs -y ${CORRETTO_RPM} && \
    mkdir /usr/java && \
    ln -s -f /usr/lib/jvm/java-11-amazon-corretto /usr/java/latest && \
    ln -s -f /usr/java/latest /usr/java/default && \
    rm -f ${CORRETTO_RPM} && \
    microdnf clean all && \
    [ ! -d /var/cache/yum ] || rm -rf /var/cache/yum && \
    [ ! -d /var/cache/dnf ] || rm -rf /var/cache/dnf

ENV JAVA_HOME /usr/java/default
ENV LANG en_US.UTF-8
EXPOSE 8080 8443

ENV PATH=$PATH:/miniconda/condabin:/miniconda/bin
ENV CONDA_HOME /miniconda
ENV CONDA_ENV vad

RUN echo "install packages missing from ubi8-minimal" \
    microdnf install --nodocs -y nss_wrapper prometheus-jmx-exporter findutils python3-devel && \
    echo "standard Spark install but adapted for microdnf from ubi8-minimal" && \
    ln -s /lib /lib64 && \
    microdnf install --nodocs -y glibc pam krb5-workstation nss && \
    mkdir -p /opt/spark && \
    mkdir -p /opt/spark/examples && \
    mkdir -p /opt/spark/work-dir && \
    touch /opt/spark/RELEASE && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd && \
    rm -rf /var/cache/apt/* && rm -rf /var/lib/apt/lists/* && \
    dnf update-minimal --nodocs -y --security --sec-severity=Important --sec-severity=Critical --skip-broken && \
    echo "C3 conda installation code" && \
    wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh && \
    bash Miniconda3-latest-Linux-x86_64.sh -b -p /miniconda && \
    rm Miniconda3-latest-Linux-x86_64.sh && \
    echo 'export PATH=$PATH:/miniconda/condabin:/miniconda/bin' >> /root/.bashrc && \
    echo "source activate vad" >> /root/.bashrc && \
    yes | conda create -n vad python=3.8 && \
    conda install -c conda-forge -n vad -y pillow=9.0.1 && \
    conda install -c conda-forge -n vad -y pandas=1.3.3 && \
    conda install -c conda-forge -n vad -y numpy=1.21.2 && \
    conda install -c conda-forge -n vad -y pystan=2.19.1.1 && \
    conda install -c conda-forge -n vad -y prophet=1.0.1 && \
    conda install -c conda-forge -n vad -y scipy=1.7.3 && \
    conda install -c conda-forge -n vad -y statsmodels=0.13.5 && \
    conda install -c conda-forge -n vad -y seaborn=0.12.2 && \
    conda install -c conda-forge -n vad -y backports=1.1 && \
    conda install -c conda-forge -n vad -y backports.zoneinfo=0.2.1 && \
    conda install -c conda-forge -n vad -y scikit-learn=1.1.1 && \
    conda install -c conda-forge -n vad -y pyarrow=9.0.0 && \
    conda clean --all --force-pkgs-dirs --yes && \
    source activate vad && pip3 install --no-cache-dir jep==3.9.1 && \
    dnf clean all && microdnf remove dnf && \
    microdnf clean all && \
    [ ! -d /var/cache/yum ] || rm -rf /var/cache/yum && \
    [ ! -d /var/cache/dnf ] || rm -rf /var/cache/dnf && \
    rm -r /usr/share/doc/

ENV LD_LIBRARY_PATH /miniconda/envs/vad/lib:/miniconda/envs/vad/lib/python3.8/site-packages/jep
ENV LD_PRELOAD /miniconda/envs/vad/lib/libpython3.8.so

# tini is installed here (not included in ubi8-minimal)
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /usr/bin/tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc /usr/bin/tini.asc
RUN chmod +x /usr/bin/tini
# end C3 customization

COPY jars /opt/spark/jars
# Copy RELEASE file if exists
COPY RELEAS[E] /opt/spark/RELEASE
COPY bin /opt/spark/bin
COPY sbin /opt/spark/sbin
COPY python /opt/spark/python
COPY kubernetes/dockerfiles/spark/entrypoint.sh /opt/
COPY kubernetes/dockerfiles/spark/decom.sh /opt/
# C3 customization - removed examples, kubernetes/tests, and data copying

ENV SPARK_HOME /opt/spark

WORKDIR /opt/spark/work-dir
RUN chmod g+w /opt/spark/work-dir
RUN chmod a+x /opt/decom.sh

ENTRYPOINT [ "/opt/entrypoint.sh" ]

# C3 customization - don't run as a specific Spark User
