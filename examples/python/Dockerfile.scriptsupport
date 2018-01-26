FROM python:3.6

ADD . /app
WORKDIR /app

RUN pip install --trusted-host pypi.python.org -r requirements.txt

EXPOSE 50051
CMD ["python", "FullScriptSupport"]