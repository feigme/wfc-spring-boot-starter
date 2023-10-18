# Makefile 介绍 https://seisman.github.io/how-to-write-makefile/index.html

# 定义检查 Docker 是否安装的命令
DOCKER_CHECK = @if ! command -v docker &> /dev/null; then \
    echo "Error: Docker is not installed. Please install Docker and try again."; \
    exit 1; \
fi

DOCKER_COMPOSE_CHECK = @if ! command -v docker-compose &> /dev/null; then \
   echo "Error: docker-compose is not installed. Please install docker-compose and try again."; \
   exit 1; \
fi

# 使用#> 规则自动获取命令介绍
.PHONY: help
help: Makefile
	@echo "Choose a command run:"
	@sed -n 's/^#>//p' $< | column -t -s ':' |  sed -e 's/^/ /'

#> make build-in-docker: 在本地环境，使用docker编译工程
.PHONY: build-in-docker
build-in-docker:
	$(DOCKER_CHECK)
	@echo "创建一个跟本地maven仓库共享的docker volume"
	docker volume create --name=maven-repo-volume --driver local --opt type=none --opt device=${HOME}/.m2/repository --opt o=bind
	@echo "在docker中编译项目"
	docker run -it --rm --name maven-build-docker \
    	-v maven-repo-volume:/root/.m2/repository \
    	-v ${PWD}:/usr/src/workspace \
    	-w /usr/src/workspace \
    	maven:3.6.3 \
    	mvn clean package '-Dmaven.test.skip=true'

#> make build-with-no-test: java打包
.PHONY: build-with-no-test
build-with-no-test:
	mvn clean package -Dmaven.test.skip -Dcheckstyle.skip -Dpmd.skip
  
#> make test: 运行测试用例
.PHONY: test
test:
	mvn test

#> make checkstyle: 运行checkstyle
.PHONY: checkstyle
checkstyle:
	mvn checkstyle:check

#> make pmd: 运行pmd
.PHONY: pmd
pmd:
	mvn pmd:pmd

#> make install: 本地打包，默认不跑测试
.PHONY: install
install:
	mvn clean install -Dpmd.skip=true -Dcheckstyle.skip=true -Dmaven.test.skip=true
