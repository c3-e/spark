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
FROM registry.c3.ai/ubi/ubi8-minimal:8.10.1783573312.1
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
ENV CORRETTO_RPM=amazon-corretto-17-x64-linux-jdk.rpm
RUN set -ex && \
    microdnf install --nodocs -y dnf wget && \
    wget https://corretto.aws/downloads/latest/${CORRETTO_RPM} && \
    dnf install --nodocs -y ${CORRETTO_RPM} && \
    rm -f ${CORRETTO_RPM}

ENV JAVA_HOME /usr/lib/jvm/java
ENV LANG en_US.UTF-8
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF-8
EXPOSE 8080 8443

ENV PATH=$PATH:/miniconda/condabin:/miniconda/bin
ENV CONDA_HOME /miniconda
ENV CONDA_ENV vad

RUN echo "install packages missing from ubi8-minimal" && \
    set -e && \
    set -x
RUN microdnf install --nodocs -y nss_wrapper prometheus-jmx-exporter findutils python3-devel && \
    echo "standard Spark install but adapted for microdnf from ubi8-minimal" && \
    ln -s /lib /lib64 && \
    microdnf install --nodocs -y glibc pam krb5-workstation nss libtiff && \
    mkdir -p /opt/spark && \
    mkdir -p /opt/spark/examples && \
    mkdir -p /opt/spark/work-dir && \
    touch /opt/spark/RELEASE && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd && \
    echo "C3 conda installation code" && \
    wget https://github.com/conda-forge/miniforge/releases/download/26.3.2-3/Miniforge3-26.3.2-3-Linux-x86_64.sh && \
    bash Miniforge3-26.3.2-3-Linux-x86_64.sh -b -p /miniconda && \
    rm Miniforge3-26.3.2-3-Linux-x86_64.sh && \
    /miniconda/bin/conda init bash && \
    echo 'export PATH=$PATH:/miniconda/condabin:/miniconda/bin' >> /root/.bashrc && \
    . /root/.bashrc && \
    conda install --override-channels -c conda-forge -y python=3.13.18 setuptools=81.0.0 msgpack-python=1.2.4 \
      urllib3=2.8.0 brotli-python=1.2.1 certifi=2026.10.23 conda-rattler-solver=26.10.1 py-rattler=0.28.1 \
      httpx2=2.16.0 && \
    /miniconda/bin/python -m pip install --no-cache-dir pip==26.3 && \
    rm -f /miniconda/conda-meta/pip-[0-9]*.json && \
    conda create --override-channels -c conda-forge -n vad python=3.11.18 -y && \
    conda activate vad && \
    conda install --override-channels -c conda-forge -y \
      pandas=1.5.3 \
      pillow=13.0.0 \
      prophet=1.1.5 \
      pyarrow=18.1.0 \
      scipy=1.13.1 \
      statsmodels=0.14.2 \
      seaborn=0.12.2 \
      matplotlib-base=3.9.4 \
      brotli-python=1.2.1 \
      certifi=2026.10.23 \
      python-dateutil=2.9.0.post0 \
      holidays=0.79 && \
    python -m pip install --no-cache-dir pip==26.3 && \
    rm -f /miniconda/envs/vad/conda-meta/pip-[0-9]*.json && \
    pip install --no-cache-dir jep==4.2.0 && \
    /miniconda/bin/python -c 'import sys; assert sys.version_info[:3] == (3, 13, 18), sys.version' && \
    python -c 'import sys; assert sys.version_info[:3] == (3, 11, 18), sys.version' && \
    python -c 'import pandas, PIL, prophet, pyarrow, scipy, statsmodels, seaborn; assert PIL.__version__ == "13.0.0", PIL.__version__' && \
    python -c 'import dateutil; assert dateutil.__version__ == "2.9.0.post0", dateutil.__version__' && \
    ls /miniconda/envs/vad/lib/python3.11/site-packages/jep/jep.cpython-311-*-linux-gnu.so && \
    test -f /miniconda/envs/vad/lib/python3.11/site-packages/jep/libjep.so && \
    test -f /miniconda/envs/vad/lib/libpython3.11.so && \
    conda list --explicit -n vad > /opt/spark/conda-spec.txt && \
    pip freeze --all > /opt/spark/pip-spec.txt && \
    test -s /opt/spark/conda-spec.txt && test -s /opt/spark/pip-spec.txt && \
    /miniconda/bin/python -m pip --version | grep -q '^pip 26\.3 ' && \
    python -m pip --version | grep -q '^pip 26\.3 ' && \
    ! ls /miniconda/conda-meta/pip-[0-9]*.json /miniconda/envs/vad/conda-meta/pip-[0-9]*.json 2>/dev/null && \
    conda clean -a -y --force-pkgs-dirs && \
    dnf clean all && \
    microdnf clean all && \
    rm -rf /var/cache/yum /var/cache/dnf && \
    rm -r /usr/share/doc/
# NOTE: never put `[ ... ] || cmd` in the && chain above: `&&` and `||` have equal precedence, so a
# failure anywhere earlier short-circuits to the `||` branch, which succeeds and hides the failure.
# The old `[ ! -d /var/cache/yum ] || ...` lines hid that `microdnf remove dnf` always failed (dnf is a
# protected package in ubi8-minimal), so dnf was never actually removed; that step is dropped, as in v8.12.
#
# Installer: Miniforge (conda-forge only, no Anaconda ToS prompt), replacing the EOL Miniconda
# py39_24.1.2 installer. The base (/miniconda) env is conda's own tooling and is pinned as in c3vad
# support/v8.12 internal/sparkImage/Dockerfile (python 3.13.18 + the setuptools/msgpack/urllib3/brotli/certifi/rattler/httpx2 CVE
# pins); pip 26.3 comes from PyPI in both envs and its stale conda-meta record is removed. No conda
# install/update may run in an env after its pip upgrade (conda would reinstall conda-forge's pip).
#
# vad env: Python 3.11 (was 3.9). Python 3.9 is EOL and its binary carries CVE-2025-4517 /
# CVE-2026-19445, and pillow 13.0.0 (CVE-2026-112393/-112398/-112403/-112900) needs python >= 3.11.
# 3.11 rather than 3.13 (as on v8.11+) because this branch stays on Spark 3.5 / pandas 1.5:
#   - PySpark 3.5 supports python <= 3.11
#   - pandas 1.5.3 has no conda-forge build newer than py311
#   - jep 4.2.0 supports python <= 3.12; keep it in lockstep with c3vad build.sbt and Jenkinsfile
# jep can't be imported from standalone python (it must be embedded in Java), so the build checks its
# native modules and the libpython3.11.so that LD_PRELOAD points at instead.
# The other vad pins are unchanged from the py3.9 image, except brotli-python/certifi (Critical
# GHSA-9v8h-qqgm-4m35 / GHSA-6q48-6c7w-9gw2) and matplotlib-base, pinned to the version the py3.9 image
# resolved so plotting output doesn't drift.
# When changing the vad env, regenerate <c3vad>/internal/buildImage/conda-spec.txt (unit-test env),
# see <c3vad>/internal/buildImage/README.md. /opt/spark/conda-spec.txt is the image's own record.

ENV LD_LIBRARY_PATH /miniconda/envs/vad/lib:/miniconda/envs/vad/lib/python3.11/site-packages/jep
ENV LD_PRELOAD /miniconda/envs/vad/lib/libpython3.11.so

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
