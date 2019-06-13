# verapdf-crawler

## Manual installation

### Pre-requisites
In order to install Logius crawler you'll need
 * Java 8, which can be downloaded [from Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html), or for
 Linux users [OpenJDK](http://openjdk.java.net/install/index.html)
 * [Heritrix 3](https://webarchive.jira.com/wiki/display/Heritrix)
 * [veraPdf](http://downloads.verapdf.org/). The latest version of veraPDF is recommended
 * [PostgreSQL 8.x](https://www.postgresql.org/download/)
 * [Python 2.x](https://www.python.org/downloads/)
 * [PDFWam checker](https://gitlab.tingtun.no/eiii_source/pdfwam/)

### Configuring Logius application
  
  * update properties in [application properties](LogiusWebApp/src/main/resources/application.properties)
  * update email for admin in [sql schema](LogiusWebApp/src/main/resources/sql/schema.sql)
   
### Configuring database
   Logius application requires connection to PostgreSQL database to store information about crawl jobs, validation jobs and processed documents.
   Connection parameters are set in application.properties file. [database schema script](LogiusWebApp/src/main/resources/sql/schema.sql) 
   and [pdf properties script](LogiusWebApp/src/main/resources/sql/pdf_properties_base_settings.sql) should be executed before first application start.

### Starting Heritrix

Heritrix should be started before the Logius application. Heritrix run command:

	heritrix_directory/bin/heritrix -a your_login:your_password -p 8443

where your_login and your_password are the Heritrix credentials defined in application.properties.

### Installing Logius web application

LogiusWebApp and HeritrixExtension modules has to be built. That can be done by using the next command in project root directory:

	mvn clean install

At the end of this operation files "LogiusWebApp", "HeritrixExtention" will be created. The "HeritrixExtention" should be moved to "lib" directory inside Heritrix.
The "LogiusWebApp" is a main Logius application jar file.

### Running Logius application
The next command will start the Logius application:

	java -jar "your_directory/LogiusWebApp/target/LogiusWebApp-1.0-SNAPSHOT.jar"
	
Default admin password: **testTEST5%**
