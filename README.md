# CRaC Example Application

Based on [github.com/CRaC/example-jetty](https://github.com/CRaC/example-jetty) to experiment with the use
of [CRaC](https://docs.azul.com/core/crac/crac-introduction).

Based on [stackabuse.com/working-with-postgresql-in-java](https://stackabuse.com/working-with-postgresql-in-java/) for
the PostgreSQL integration.

## Goal of the Application

This demo application shows a few use-cases:

* Provides a HTTP endpoint to load data from zipped CSV files and return the first 100 records.
* The data is converted to Java objects, which takes a long time for a large dataset.
* After first load of a file, the data is stored in memory, so each new request returns quickly.
* A database is used to store the duration of all actions. The stored logs can also be retrieved with the HTTP endpoint.

Related to CRaC:

* The database connection is closed before checkpoint, and reopened after restore.
* Because the CSV data is stored in memory, it demonstrates that the application returns data immediately in the HTTP
  calls after restore from checkpoint, as it's not needed to reload from CSV. This is different behaviour compared to
  starting the application from JAR.

HTTP endpoints:

* Are listed on the page at http://localhost:8080.
* http://localhost:8080/files/organizations-1000.csv
* http://localhost:8080/files/organizations-10000.csv
* http://localhost:8080/files/organizations-100000.csv
* http://localhost:8080/files/organizations-500000.csv
* http://localhost:8080/files/organizations-1000000.csv
* http://localhost:8080/logs

## Results

These are the durations needed for the HTTP endpoints to respond, as logged in the database.

### When Started from JAR

```text
duration=11800, description=Handled request for /files/organizations-1000000.csv
duration=8721, description=Data was converted to Java objects from organizations-1000000.csv
duration=3054, description=ZIP was unpacked from organizations-1000000.csv
duration=5197, description=Handled request for /files/organizations-500000.csv
duration=3609, description=Data was converted to Java objects from organizations-500000.csv
duration=1567, description=ZIP was unpacked from organizations-500000.csv
duration=1482, description=Handled request for /files/organizations-100000.csv
duration=1080, description=Data was converted to Java objects from organizations-100000.csv
duration=372, description=ZIP was unpacked from organizations-100000.csv
duration=373, description=Handled request for /files/organizations-10000.csv
duration=309, description=Data was converted to Java objects from organizations-10000.csv
duration=35, description=ZIP was unpacked from organizations-10000.csv
duration=222, description=Handled request for /files/organizations-1000.csv
duration=94, description=Data was converted to Java objects from organizations-1000.csv
duration=7, description=ZIP was unpacked from organizations-1000.csv
duration=0, description=Started from main
duration=0, description====================================================]
```

### When Started from Checkpoint

As unzipping and data conversion to Java objects is was already done before creating the checkpoint, the HTTP endpoint
can respond immediately. For the largest file this means a response in 7 milliseconds instead of 11800.

```text
duration=7, description=Handled request for /files/organizations-1000000.csv
duration=7, description=Handled request for /files/organizations-500000.csv
duration=6, description=Handled request for /files/organizations-100000.csv
duration=8, description=Handled request for /files/organizations-10000.csv
duration=15, description=Handled request for /files/organizations-1000.csv
duration=0, description=Reopened DB connection after restore
duration=0, description====================================================
```

### Conclusion

Of course, this time-consuming CSV-reading process is not a typical use-case, but is used here to illustrate how
time-consuming processes can be stored in a checkpoint.

## Example Test Data

The CSV files located in `src/main/resources/data` were downloaded
from [datablist.com](https://www.datablist.com/learn/csv/download-sample-csv-files).

## Requirements for Raspberry Pi

### OS and Java

As described on the blog
post [Running a CRaC Java application on Raspberry Pi - UPDATE](https://webtechie.be/post/2023-10-16-crac-on-raspberry-pi-update/).

* Raspberry Pi OS, 64-bit, Bookworm edition, released on October 11, 2023.
* Azul Zulu Builds of OpenJDK, version 21 with CRaC:

```bash
$ sdk install java 21.crac-zulu
```

### PostgreSQL

Install PostgreSQL and create a user `cracApp` with password `crac123`, and database `crac` for the demo application.

```bash
$ sudo apt install postgresql
$ sudo su postgres
postgres$ createuser cracApp -P --interactive
# use password crac123
# superuser y
postgres$ psql
postgres=# CREATE DATABASE crac;
postgres=# exit
postgres$ exit
$ exit
```

Check if PostgreSQL is running, and the port (5432 by default):

```bash
$ sudo netstat -plunt |grep postgres
tcp        0      0 127.0.0.1:5432          0.0.0.0:*               LISTEN      20715/postgres
tcp6       0      0 ::1:5432                :::*                    LISTEN      20715/postgres

$ service postgresql status
● postgresql.service - PostgreSQL RDBMS
     Loaded: loaded (/lib/systemd/system/postgresql.service; enabled; preset: enabled)
     Active: active (exited) since Tue 2023-10-17 15:01:28 BST; 7min ago
   Main PID: 20491 (code=exited, status=0/SUCCESS)
        CPU: 3ms

Oct 17 15:01:28 crac systemd[1]: Starting postgresql.service - PostgreSQL RDBMS...
Oct 17 15:01:28 crac systemd[1]: Finished postgresql.service - PostgreSQL RDBMS.
```

If you want to connect to the database from another PC, you need to allow remote connections from all addresses. **This
is not secure**, and should only be used for test/develop/debug purposes!

```bash
$ sudo nano /etc/postgresql/15/main/postgresql.conf

# Add this line
listen_addresses = '*'

$ sudo nano /etc/postgresql/15/main/pg_hba.conf

# Comment this line and add a new one
#host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             all            scram-sha-256

$ sudo service postgresql restart
```

Create a table to store the logs:

```postgresql
create table app_log
(
    id          integer NOT NULL GENERATED ALWAYS AS IDENTITY (START 1 INCREMENT 1),
    timestamp   timestamp with time zone DEFAULT now(),
    duration    integer NOT NULL,
    description character varying(255),
    CONSTRAINT app_log_pkey PRIMARY KEY (id)
);
```

## Run with CRaC on Raspberry Pi

### Get the Project

```bash
$ git clone https://github.com/FDelporte/crac-example.git
$ cd crac-example
```

### Build and Initial Run

#### First Terminal

This Pi4J application needs to be executed as sudo to have the needed privileges to interact with the GPIOs.

```bash
$ mvn package
$ java -XX:CRaCCheckpointTo=cr -jar target/crac-example.jar
```

#### Second Terminal

```bash
$ jcmd target/crac-example.jar JDK.checkpoint
```

In the first terminal, you can see what's happening during the checkpoint creation:

```text
Oct 17, 2023 7:21:29 PM jdk.internal.crac.LoggerContainer info
INFO: Starting checkpoint
17/10/2023 19:21 | ServerManager                       | beforeCheckpoint     | INFO     | Executing beforeCheckpoint
2023-10-17 19:21:29.978:INFO:oejs.AbstractConnector:Attach Listener: Stopped ServerConnector@77a98a6a{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
17/10/2023 19:21 | DatabaseManager                     | beforeCheckpoint     | INFO     | Executing beforeCheckpoint
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/log4j-api-2.20.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/log4j-core-2.20.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/checker-qual-3.31.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/postgresql-42.6.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/crac-1.4.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/jetty-io-9.4.51.v20230217.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/jetty-util-9.4.51.v20230217.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/jetty-http-9.4.51.v20230217.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/javax.servlet-api-3.1.0.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/dependency/jetty-server-9.4.51.v20230217.jar is recorded as always available on restore
Oct 17, 2023 7:21:31 PM jdk.internal.crac.LoggerContainer info
INFO: /home/crac/crac-example/target/crac-example.jar is recorded as always available on restore
Killed
```

### Start From Snapshot

```bash
$ java -XX:CRaCRestoreFrom=cr

17/10/2023 19:22 | DatabaseManager                     | afterRestore         | INFO     | Executing afterRestore
17/10/2023 19:22 | DatabaseManager                     | initConnection       | WARN     | Setting up database connection
17/10/2023 19:22 | DatabaseManager                     | initConnection       | INFO     | Database connection status: {ApplicationName=PostgreSQL JDBC Driver}
17/10/2023 19:22 | ServerManager                       | afterRestore         | INFO     | Executing afterRestore
2023-10-17 19:22:48.800:INFO:oejs.AbstractConnector:Attach Listener: Started ServerConnector@77a98a6a{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
```


