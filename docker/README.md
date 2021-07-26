# *BeeDeeM* and *Docker*

This document explains how you can setup and use *BeeDeeM* within a Docker container.

## Requirements

Of course, you need to have [Docker](https://docs.docker.com/engine/installation/) installed on your system. 

We also suppose that you are familiar with [docker build](https://docs.docker.com/engine/reference/commandline/build/) and [docker run](https://docs.docker.com/engine/reference/commandline/run/) commands.

Note: this BeeDeeM's *Dockerfile* was made and tested using *Docker version 17* on *Mac OSX El Capitan*. 

## Build the container

Use this command: 
  
     docker build -f Dockerfile -t beedeem_machine .

      --> don't forget the final '.' in the cmdline

## Run the container

     docker run --name beedeem_machine -i -t --rm \
                -v /path/to/bank/installation:/beedeem-db \  <-- (1)
                -v /path/to/work/dir:/beedeem-wk \           <-- (2)
                beedeem_machine <command-line>               <-- (3)
      
      (1) where to install banks. Update '/path/to/...' to target your local system. 
          DO NOT MODIFY '/beedeem-db'.
      (2) where to put BeeDeeM logs. Update '/path/to/...' to target your local system. 
          DO NOT MODIFY '/beedeem-wk'.
      (3) what to do. See 'Sample use cases', below.


### Sample use cases
 
1/ install a bank:
 
      docker run .../... beedeem_machine install.sh -desc PDB_proteins
 
Will invoke 'install.sh' BeeDeeM script. See [BeeDeeM user manual](https://pgdurand.gitbooks.io/beedeem/test_install.html\#install-a-bank) for more details. 


### Monitor BeeDeeM
   
In all cases, consult BeeDeeM working directory to check out log files in case command does not work as expected.
 
This working directory is specified by this 'docker run' argument:

           -v /path/to/work/dir:/beedeem-wk

Which means that BeeDeeM log files can be located on your system within '/path/to/work/dir'. 

If needeed, you can tell BeeDeeM to dump logs directly on the console using this command:

      docker run .../... -e "KL_LOG_TYPE=console" beedeem_machine install.sh -desc PDB_proteins

### Default JRE memory usage

Java is pre-configured to use up to 2 Gb RAM. You can change this by adding such an argument to your docker run command:

      docker run .../... -e "KL_JRE_ARGS=-Xms128M -Xmx1G -Djava.io.tmpdir=/beedeem-wk" beedeem_machine install.sh -desc PDB_proteins

Tips: ALWAYS redirect appropriately JRE tmp directory to somewhere outside the container! This is the reason why you see a -Djava.io.tmpdir directive in the previous command.

### Here is a working command on my OSX computer:

1. I created these directories:

docker run --name beedeem_machine -i -t --rm -v /Users/pgdurand/biobanks:/beedeem-db -v /Users/pgdurand/biobanks/tmp:/beedeem-wk -e "KL_LOG_TYPE=console" beedeem_machine install.sh -desc PDB_proteins

         /Users/pgdurand/biobanks  (1)
         /Users/pgdurand/biobanks/log      (2)

           (1) will host my banks on my computer
           (2) will host BeeDeeM log files on my computer

2. Then I can install a bank as follows:

         docker run --name beedeem_machine -i -t --rm \
                    -v /Users/pgdurand/biobanks:/beedeem-db \
                    -v /Users/pgdurand/biobanks/log:/beedeem-wk \
                    beedeem_machine \
                    install.sh -desc PDB_proteins

In that case, BeeDeeM installs bank within directory '/beedeem-db', which actually targets '/Users/pgdurand/biobanks' through the Docker container. In a similar way, BeeDeeM creates a log file within '/beedeem-wk', which is actually '/Users/pgdurand/biobanks/log'.

## Additional notes
 
### Root access inside the container

You'll be able to enter into the container, as follows:

     - if running: docker exec -it beedeem_machine bash

     - if not yet running: docker run --rm -i -t beedeem_machine bash
