## Build the Docker image(s)
To run the example plugins in the way described here, you need to first install [Docker](https://www.docker.com/).

 From the examples/python folder, execute one of the following lines according to which plugin you want to build:
 

 

 - docker build -t qlik-oss/sse-scriptsupport -f Dockerfile.scriptsupport .
 - docker build -t qlik-oss/sse-helloworld -f Dockerfile.helloworld .
 - docker build -t qlik-oss/sse-columnoperations -f Dockerfile.columnoperations .



## Run the Docker container(s)
Execute one of the following commands to run the plugin of choice in a docker container:

 

 - docker run --rm -p 50051:50051 qlik-oss/sse-scriptsupport
 - docker run --rm -p 50052:50052 qlik-oss/sse-helloworld
 - docker run --rm -p 50053:50053 qlik-oss/sse-columnoperations
