<!-- TOC -->
  * [Java Installation using Installer](#java-installation-using-installer)
  * [Install the Latest Version of JAVA using SDK man](#install-the-latest-version-of-java-using-sdk-man)
    * [Install sdkMan](#install-sdkman)
    * [Install Java using sdk man](#install-java-using-sdk-man)
      * [How to install a specific Java Version ?](#how-to-install-a-specific-java-version-)
        * [Java 20](#java-20)
        * [Java 21](#java-21)
<!-- TOC -->

## Java Installation using Installer

- Download the latest java from the below link
    - [Java 20](https://www.oracle.com/java/technologies/javase/jdk20-archive-downloads.html)
    - [Java 21](https://www.oracle.com/java/technologies/downloads/)

## Install the Latest Version of JAVA using SDK man

### Install sdkMan

- Follow the instructions in the below link to install sdkman in your mac.
    - [sdkMan](https://sdkman.io/install)

### Install Java using sdk man

- Run the below command to view the different version of supported Java
```agsl
sdk list java
```
#### How to install a specific Java Version ?

##### Java 20

```linux
sdk list java | grep '20'
```
- Running the below command will install Java 20.

```linux
sdk install java 20.0.2-tem
```

##### Java 21

```linux
sdk list java | grep '21'
```

## Set up Insomnia Rest Client

- Navigate to this link ->  https://github.com/ArchGPT/insomnium
- Download and Install the appropriate OS compatible version.

### Set up the HTTP Request collection.

- Open the insomnia Rest Client
- Import the [Insomnia-http-collection.json](Insomnia-http-collection.json) file.
