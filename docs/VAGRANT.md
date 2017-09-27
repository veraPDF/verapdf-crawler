Vagrant and Ansible setup
=========================
Instructions for setting up your own instance of the Logius/veraPDF. If you want to start with a local development instance, which is advisable, you'll need VirtualBox and Vagrant to automate the management of the local development VM. To roll an instance of the application to a fresh server you only need to install Ansible and to follow the instructions.

Server Stack
------------
The main stack required to run the application is as follows:
- Debian
- nginx
- MySQL for database
- Java 8 for Heritrix and the application
- Heritrix
- veraPDF

In order to update the application the Ansible task also installs:
- Git in order to update the project source code
- Maven 3 to build Java projects
- The unzip utility for unpacking the veraPDF installer

Pre-requisites
--------------
This is the only software that's necessary to allow you to roll out to a remote machine via SSH. VirtualBox and Vagrant provide virtualization and automation to allow you to roll out to a virtual machine on your own PC.

### Ansible
Ansible, see https://www.ansible.com/get-started, is used to automate the application deployment, both to the development VM as well as any Debian server where you have SSH access. You need to install Ansible locally but it doesn't require anything installing on the target system.

You can now roll out the server application to a remote server. If this is what you want to do you can ignore the sections on VirtualBox and Vagrant below.

### VirtualBox
We've chosen VirtualBox, see https://www.virtualbox.org/, as the base virtualization layer as it provides cross platform virtualization for most standard dev setups. You can download a version for Windows, Mac OS and Linux from [here](https://www.virtualbox.org/wiki/Downloads). You'll also need the VirtualBox extension pack http://download.virtualbox.org/virtualbox/5.1.24/Oracle_VM_VirtualBox_Extension_Pack-5.1.24-117012.vbox-extpack.

### Vagrant
Vagrant, see https://www.vagrantup.com/, is used to automate the provisioning of the VirtualBox VM. It effectively creates a vanilla Debian Jessie box then uses Ansible (below) to carry out the configuration and installations necessary to set up the crawler box.

Vagrant is available for Windows, Mac OS and Linux, you can download the latest version [here](https://www.vagrantup.com/downloads.html) and follow [these installation instructions](https://www.vagrantup.com/docs/installation/).

What's Installed Where?
-----------------------
Out of the box defaults with install the applications in the following locations:

```
\opt\
    |
    |- heritrix-3.2.0
    |- maven
    |- verapdf-greenfield-1.8.4
\var\
    |
    |- lib\
        |
        |- logius
```

Quick Starts
------------

### Vagrant
Once you have the pre-requisites installed, to start up a local Vagrant instance issue the following command from anywhere below the project root:

```bash
vagrant up
```
This will create and configure the VirtualBox and then used Ansible to install the project software stack. Once that's completed you can SSH to the vagrant machine by:
```bash
vagrant ssh
```
and run the setup scripts.

Ansible
-------

### Getting your facts straight
As you use Ansible you'll want to know what state it thinks your machine is in. Ansible tracks this information using facts and you can ask for a fact check at any time. The machine in question needs to be running and you'll need SSH access. For the development vagrant instance issue this command will do the trick from the project root:

```bash
ansible -i ansible/development --key-file=verapdf-crawler/.vagrant-wrkstn/machines/default/virtualbox/private_key -u vagrant -m setup logius.verapdf.dev
```

A quick explanation of the options:
- `-i` points to the Ansible inventory file that describes the machines to be acted on, in this case `ansible/development` for the vagrant VM.
- `--keyfile` points to the private key file needed to access the machine, here we point to the vagrant machines private key file `verapdf-crawler/.vagrant-wrkstn/machines/default/virtualbox/private_key.`
- `-u` the remote ssh user, for vagrant machines we use the default vagrant user
- -m requests the setup module only been run

### Roles, Inventories and Playbooks
The key Ansible concepts are roles, inventories and playbooks. We'll briefly give you enough information to get started.

#### Roles
An Ansible role automates a particular task such as a software installation, updating system libraries, etc. These are generally server admin / development tasks. Examples would be setting the time-zone on a server, or installing the veraPDF software. The roles developed by us can be found under the [ansible/roles directory](./ansible/roles). Once you run the playbook the third party roles used in the installation will be downloaded here also. The download is driven by the inventory of third party roles in the [ansible/roles/roles.txt file](./ansible/roles/roles.txt).

#### Playbooks
Roles are modular, to perform useful tasks they're organised into playbooks. Playbooks link set's of roles to particular machines. The playbook to install the Logius crawler software is the [ansible/logius-crawler.yml](ansible/logius-crawler.yml) file. A snapshot of the file is shown below:
```yaml
---

# Playbook to configure a logius-crawler instance
# File: ansible/site.yml


- hosts: all
  become: true
  roles:
    - { role: tschifftner.hostname }
    - { role: tersmitten.timezone }
    - { role: tersmitten.apt }

- hosts: heritrix
  become: true
  roles:
    - { role: robdyke.maven }
    - { role: geerlingguy.mysql }
    - { role: HanXHX.nginx }
    - { role: verapdf.heritrix }
    - { role: verapdf.logius }
    - { role: verapdf.verapdf }
```

It states that all servers will have the hostname, timezone and apt setup. The main installation will only take place for machines in the heritrix group.

#### Inventories and Host Variables
So playbooks bring roles together to carry out tasks on machines. Inventories provide lists of machines and can be pretty basic. The one for the development roll out is here [ansible/development](ansible/development) and it looks something like:
```yaml
[servers:children]
heritrix

[heritrix]
logius.verapdf.dev
```

This sets up a group called `servers` with a child group of `heritrix`. The `heritrix` group contains a single machine `logius.verapdf.dev`. This name provides a link to the host variables file with a matching name, [ansible/host_vars/logius.verapdf.dev](ansible/host_vars/logius.verapdf.dev). This is set to roll out a development instance to a local vagrant virtual machine. This is where the task configuration happens, from the IP address of the target server to installation directories for specific apps.

### Running a Playbook on a host
You can use the `ansible-playbook` command to run a playbook against a particular inventory. This would work on the vagrant machine, except SSH would complain:

```bash
ansible-playbook ansible/logius-crawler.yml -i ansible/development --key-file=verapdf-crawler/.vagrant-wrkstn/machines/default/virtualbox/private_key -u vagrant
```
A server roll-out would look like this:
```bash
ansible-playbook ansible/logius-crawler.yml -i ansible/development  --extra-vars "ansible_sudo_pass=**YOUR PASSWORD HERE**"
```

### Configuration
We provide a configuration for the vagrant box, at [ansible/host_vars/logius.verapdf.dev](ansible/host_vars/logius.verapdf.dev). Bear in mind this is intended as a development box. You'll probably not want to expose the Heritrix admin GUI via nginx on public boxes.

The configuration file is pretty well documented and could be used as the basis of a production server roll out, we use it for ours.

### OTS Ansible Roles Overview
These are the off the shelf Ansible roles from Ansible Galaxy used to set up the debian box.

#### tersmitten.hostname
Sets the remote machine hostname, deals with /etc/hostname and the like.

#### tersmitten.apt
Performs apt cache update, dist-update and installs apt modules.

#### tersmitten.timezone
Sets up server timezone.

### robdyke.maven
Installs an up to date version of Maven, the default Jessie version lacks support for particular plugins.

#### geerlingguy.mysql
Installs and congigures MySQL database used as an application DB.

#### HanXHX.nginx
Installs the nginx web server used to provide external access to the application web GUI and, optionally, the Heritrix admin GUI.

### Bespoke Ansible Roles Overview

#### verapdf.heritrix
Installs version 3.2.0 of Heritrix and, optionally, Java 8 support. The configuration options can be found in [the role's default values file](ansible/roles/verapdf.heritrix/defaults/main.yml). The main config options are:
- `heritrix_install_java` which should be set `true` if you require Java 8 installation, default `false`;
- `heritrix_nginx_access` which should be set `true` if you want to provide external access to the Heritrix admin site via nginx, default `false`; and
- `heritrix_install_root` the name of an existing directory for the installation parent directory, default `/opt`.

The role also generates the Java key files needed for Heritrix to run under Java 8. A scripted sketch of the role looks like:
```bash
# Download and unpack Heritrix
cd /opt
wget http://builds.archive.org/maven2/org/archive/heritrix/heritrix/3.2.0/heritrix-3.2.0-dist.tar.gz
tar -xzvf heritrix/3.2.0/heritrix-3.2.0-dist.tar.gz
export HERITRIX_HOME=/opt/heritrix-3.2.0
chmod u+x $HERITRIX_HOME/bin/heritrix

# Generate Java Keys
cd $HERITRIX_HOME
keytool -keystore adhoc.keystore -storepass password -keypass password -alias adhoc -genkey -keyalg RSA -dname "CN=Heritrix Ad-Hoc HTTPS Certificate" -validity 3650

# Generate self certs for nginx
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout  /etc/nginx/cert.key -out /etc/nginx/cert.crt
```

Troubleshooting
---------------

### VirtualBox VM is unreachable by ansible SSH
Your attempt to provision the VM ends with something similar to this:

```bash
fatal: [logius.verapdf.dev]: UNREACHABLE! => {
    "changed": false,
    "msg": "Failed to connect to the host via ssh: OpenSSH_7.2p2 Ubuntu-4ubuntu2.2, OpenSSL 1.0.2g  1 Mar 2016\r\ndebug1: Reading configuration data /home/cfw/.ssh/config\r\ndebug1: Reading configuration data /etc
/ssh/ssh_config\r\ndebug1: /etc/ssh/ssh_config line 19: Applying options for *\r\ndebug1: auto-mux: Trying existing master\r\ndebug1: Control socket \"/home/cfw/.ansible/cp/2d3a7192ca\" does not exist\r\ndebug2: r
esolving \"192.168.10.10\" port 22\r\ndebug2: ssh_connect_direct: needpriv 0\r\ndebug1: Connecting to 192.168.10.10 [192.168.10.10] port 22.\r\ndebug2: fd 3 setting O_NONBLOCK\r\ndebug1: connect to address 192.168
.10.10 port 22: Connection timed out\r\nssh: connect to host 192.168.10.10 port 22: Connection timed out\r\n",
    "unreachable": true
}
```
then you might need to restart the network on your host system: `sudo ip link set up dev vboxnet0`
