veraPDF Logius Services
=======================

Introduction
------------
If you install the veraPDF crawler application using Ansible the applications will be installed as a set of Linux Services. This document gives a quick overview of what lives where and what you'll need to do to get the application up and running.

Services
--------
The Ansible tasks set up three services:
- logius-web: the crawler application web GUI, required;
- logius-sample: a sample web application with some test files, optional; and
- verapdf-service: REST service wrapper for veraPDF tool, required.

Additionally you'll need Heritrix running, we're working on reliability and integration here. Heritrix isn't developed by our team and it's proving a little resistant to daemonisation.

### Running Heritrix
To 
