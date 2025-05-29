# Dockerfile.jenkins
FROM jenkins/jenkins:lts

USER root

# 1) Установим зависимости
RUN apt-get update && apt-get install -y \
      ca-certificates \
      curl \
      gnupg \
      lsb-release \
      python3-pip \
    && rm -rf /var/lib/apt/lists/*

# 2) Добавим официальный GPG-ключ и репозиторий Docker CE
RUN mkdir -p /etc/apt/keyrings \
  && curl -fsSL https://download.docker.com/linux/debian/gpg \
       | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
  && echo \
     "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
     https://download.docker.com/linux/debian \
     $(lsb_release -cs) stable" \
     > /etc/apt/sources.list.d/docker.list

# 3) Установим docker-cli и плагин compose
RUN apt-get update && apt-get install -y \
      docker-ce-cli \
      docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

# 4) Дадим пользователю jenkins доступ к Docker socket
RUN groupadd --system docker || true \
  && usermod -aG docker jenkins

# Переключимся обратно на пользователя jenkins
USER jenkins
