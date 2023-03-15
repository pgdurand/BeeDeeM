# *BeeDeeM* and *Docker*

This document explains how you can setup and use *BeeDeeM* within a Docker container.

## Requirements

Of course, you need to have [Docker](https://docs.docker.com/engine/installation/) installed on your system. 

We also suppose that you are familiar with [docker build](https://docs.docker.com/engine/reference/commandline/build/) and [docker run](https://docs.docker.com/engine/reference/commandline/run/) commands.

Note: this BeeDeeM's *Dockerfile* was made and tested using *Docker engine release 20* on *macOS Monterey*. 

## Get a pre-built image

Simply use:

```docker pull sebimer/beedeem:5.0.0```

## Build the container

Use this command: 
  
     docker build -f Dockerfile -t beedeem_machine .

      --> don't forget the final '.' in the cmdline

## Run the container

     KL_mirror__path=/path/to/bank_repository    <-- (1)
     KL_WORKING_DIR=/path/to/work-directory      <-- (2)
     KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=${KL_WORKING_DIR} -DKL_LOG_TYPE=console"  <-- (3)

     docker run --name beedeem_machine -i -t --rm \
                -e \"KL_JRE_ARGS=$KL_JRE_ARGS\" \
                -e \"KL_WORKING_DIR=$KL_WORKING_DIR\" \
                -e \"KL_mirror__path=$KL_mirror__path\" \ 
                -v /path/to/bank/installation:/beedeem-db \ 
                -v /path/to/work/dir:/beedeem-wk \ 
                beedeem_machine <command-line>       <-- (4) 
      
      (1) where to install banks. Update '/path/to/...' to target your local system. 
      (2) where to put BeeDeeM logs. Update '/path/to/...' to target your local system. 
      (3) Arguments to run Java Runtime Environment (BeeDeeM is a Java software)
      (4) what to do. See 'Sample use cases', below.

You can review the 'test_container.sh" script to look at a working exemple.

### Sample use cases
 
1/ install a simple bank:
 
      docker run .../... beedeem_machine bdm install -desc PDB_proteins
 
Will invoke 'bdm' BeeDeeM script with command 'install'. See [BeeDeeM user manual](https://pgdurand.gitbooks.io/beedeem/test_install.html\#install-a-bank) for more details. 

2/ install an annotated bank:
 
      docker run .../... beedeem_machine bdm install -desc SwissProt_human

3/ get list of installed banks:
 
      docker run .../... beedeem_machine bdm info -d all -f txt

4/ query a bank to fetch an entry:
 
      docker run .../... beedeem_machine bdm query -d protein -i P31946 -f txt

If it fails, just try this form:

      docker run .../... beedeem_machine bdm query protein P31946 txt
      

### Monitor BeeDeeM
   
In all cases, consult BeeDeeM working directory to check out log files in case command does not work as expected.
 
This working directory is specified by this 'docker run' argument:

           -v /path/to/work/dir:/path/to/work/dir

Which means that BeeDeeM log files can be located on your system within '/path/to/work/dir'. 

If needeed, you can tell BeeDeeM to dump logs directly on the console using this command:

      docker run .../... -e "KL_LOG_TYPE=console" beedeem_machine bdm install -desc PDB_proteins

### Default JRE memory usage

Java is pre-configured to use up to 2 Gb RAM. You can change this by adding such an argument to your docker run command:

      docker run .../... -e "KL_JRE_ARGS=-Xms128M -Xmx1G -Djava.io.tmpdir=/path/to/work/dir" beedeem_machine bdm install -desc PDB_proteins

Tips: ALWAYS redirect appropriately JRE tmp directory to somewhere outside the container! This is the reason why you see a -Djava.io.tmpdir directive in the previous command.

## Additional notes
 
### Root access inside the container

You'll be able to enter into the container, as follows:

     - if running: docker exec -it beedeem_machine bash

     - if not yet running: docker run --rm -i -t beedeem_machine bash

### Convert Docker image to Singularity

```
docker save beedeem-<ver> -o beedem.tar
singularity build beedeem-<ver>.sif docker-archive://beedeem.tar
```

