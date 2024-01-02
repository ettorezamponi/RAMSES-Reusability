FROM mysql:latest
RUN chown -R mysql:root /var/lib/mysql/
ENV MYSQL_ROOT_PASSWORD=root
COPY script.sql /docker-entrypoint-initdb.d/1_script.sql
