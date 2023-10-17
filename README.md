# CRaC Example Application

Based on [github.com/CRaC/example-jetty](https://github.com/CRaC/example-jetty) to experiment with the use
of [CRaC](https://docs.azul.com/core/crac/crac-introduction).

## Example Test Data

Is downloaded from [datablist.com](https://www.datablist.com/learn/csv/download-sample-csv-files).

## Requirements for Raspberry Pi

As described on the blog
post [Running a CRaC Java application on Raspberry Pi - UPDATE](https://webtechie.be/post/2023-10-16-crac-on-raspberry-pi-update/).

* Raspberry Pi OS, 64-bit, Bookworm edition, released on October 11, 2023.
* Azul Zulu Builds of OpenJDK, version 17 or 21 with CRaC:
    * `17.0.8.crac-zulu`
    * `21.crac-zulu`
    * or newer

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


