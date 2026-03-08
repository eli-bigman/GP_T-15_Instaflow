### getting password for airflow
`docker exec -it airflow-webserver cat /opt/airflow/simple_auth_manager_passwords.json.generated`

### testing ingestion into MinIO
`docker exec -it airflow-scheduler python /opt/airflow/dags/utils/test_minio.py`

### Testing postgres metadata part
`docker exec -it airflow-scheduler python /opt/airflow/dags/utils/test_metadata.py`

### Check the metadata table
`# List tables in the metadata schema specifically
docker exec -it postgres-airflow psql -U airflow -d airflow -c "\dt metadata.*"`


### Get parquet data from MinIO
 SUMMARIZE SELECT * FROM read_parquet('s3://bronze/**/*.parquet', union_by_name=true);

### Check if dag has not issues
docker exec -it airflow-dag-processor python -c "import ast; ast.parse(open('/opt/airflow/dags/silver_dag.py').read()); print('OK')"

# Drop silver table
docker exec -it postgres-airflow psql -U airflow -d airflow -c "DROP TABLE IF EXISTS silver.sales;"

# Reset all files to NEW
docker exec -it postgres-airflow psql -U airflow -d airflow -c "UPDATE metadata.ingestion_metadata SET status = 'NEW', updated_at = NOW();"

# Reset only FAILED files to NEW
docker exec -it postgres-airflow psql -U airflow -d airflow -c "UPDATE metadata.ingestion_metadata SET status = 'NEW', updated_at = NOW() WHERE status = 'FAILED';"

# Check current status
docker exec -it postgres-airflow psql -U airflow -d airflow -c "SELECT file_id, status FROM metadata.ingestion_metadata;"