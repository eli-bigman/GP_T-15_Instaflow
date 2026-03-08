FROM apache/airflow:3.0.1-python3.11

USER root

COPY requirements.txt /requirements.txt
RUN chmod 644 /requirements.txt

# Use default Airflow user to install dependencies
USER 50000

RUN pip install --no-cache-dir apache-airflow==3.0.1 -r /requirements.txt