FROM azul/zulu-openjdk:8
RUN apt-get update && apt-get install -y curl apt-transport-https \
                                                  ca-certificates \
                                                  curl \
                                                  gnupg2 \
                                                  software-properties-common \
                                                  wget
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
RUN add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
       $(lsb_release -cs) \
       stable"
RUN (curl -sL https://deb.nodesource.com/setup_12.x |  bash - )
RUN apt-get update && apt-get install -y docker-ce-cli nodejs
RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
ARG USER="stresstester"
RUN useradd -m ${USER}