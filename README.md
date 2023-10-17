# CRaC Example Application

Based on [github.com/CRaC/example-jetty](https://github.com/CRaC/example-jetty) to experiment with the use
of [CRaC](https://docs.azul.com/core/crac/crac-introduction).

Based on [stackabuse.com/working-with-postgresql-in-java](https://stackabuse.com/working-with-postgresql-in-java/) for the PostgreSQL integration.

## Example Test Data

Is downloaded from [datablist.com](https://www.datablist.com/learn/csv/download-sample-csv-files).

## Requirements for Raspberry Pi

### OS and Java

As described on the blog
post [Running a CRaC Java application on Raspberry Pi - UPDATE](https://webtechie.be/post/2023-10-16-crac-on-raspberry-pi-update/).

* Raspberry Pi OS, 64-bit, Bookworm edition, released on October 11, 2023.
* Azul Zulu Builds of OpenJDK, version 17 or 21 with CRaC:
    * `17.0.8.crac-zulu`
    * `21.crac-zulu`
    * or newer

### PostgreSQL

Install PostgreSQL and create a user and database for the test application.

```bash
$ sudo apt install postgresql
$ sudo su postgres
postgres$ createuser cracApp -P --interactive
# use password crac123
# superuser y
$ psql
postgres=# CREATE DATABASE crac;
postgres=# exit
postgres$ exit

sudo netstat -plunt |grep postgres
tcp        0      0 127.0.0.1:5432          0.0.0.0:*               LISTEN      20715/postgres
tcp6       0      0 ::1:5432                :::*                    LISTEN      20715/postgres
service postgresql status
● postgresql.service - PostgreSQL RDBMS
     Loaded: loaded (/lib/systemd/system/postgresql.service; enabled; preset: enabled)
     Active: active (exited) since Tue 2023-10-17 15:01:28 BST; 7min ago
   Main PID: 20491 (code=exited, status=0/SUCCESS)
        CPU: 3ms

Oct 17 15:01:28 crac systemd[1]: Starting postgresql.service - PostgreSQL RDBMS...
Oct 17 15:01:28 crac systemd[1]: Finished postgresql.service - PostgreSQL RDBMS.
```

If you want to connect to the database from another PC:

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

Create a table for the logs

```postgresql
create table app_log
(
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY (START 1 INCREMENT 1 ),
    timestamp timestamp with time zone DEFAULT now(),
    duration integer NOT NULL,
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

### Start From Snapshot

```bash
$ java -XX:CRaCRestoreFrom=cr
```


