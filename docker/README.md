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
                -v /path/to/bank/installation:/biobase \  <-- (1)
                -v /path/to/work/dir:/var/beedeem \       <-- (2)
                beedeem_machine <command-line>            <-- (3)
      
      (1) where to install banks. Update '/path/to/...' to target your local system. 
          DO NOT MODIFY '/biobase'.
      (2) where to put BeeDeeM logs. Update '/path/to/...' to target your local system. 
          DO NOT MODIFY '/var/beedeem'.
      (3) what to do. See 'Sample use cases', below.


### Sample use cases
 
1/ install a bank:
 
      docker run .../... beedeem_machine -c install swiss
 
Will invoke 'install.sh' BeeDeeM script. See [BeeDeeM user manual](https://pgdurand.gitbooks.io/beedeem/test_install.htmlinstall-a-bank) for more details. 

2/ query a bank: 
 
      docker run .../... beedeem_machine -c query protein 1433S_HUMAN txt
 
Will invoke 'query.sh' BeeDeeM script. See [BeeDeeM user manual](https://pgdurand.gitbooks.io/beedeem/test_install.htmlquery-the-beedeem-bank-repository) for more details. 

3/ annotate a BLAST result: 
 
      docker run .../... beedeem_machine -c annotate 1433S_HUMAN.blastp 1433S_HUMAN.zml full
 
Will invoke 'annotate.sh' BeeDeeM script.See [BeeDeeM user manual](https://pgdurand.gitbooks.io/beedeem/test_install.htmlrun-a-blast-search) for more details. 

### Monitor BeeDeeM
   
In all cases, consult BeeDeeM working directory to check out log files in case command does not work as expected.
 
This working directory is specified by this 'docker run' argument:

           -v /path/to/work/dir:/var/beedeem

Which means that BeeDeeM log files can be located on your system within '/path/to/work/dir'.

### Here is a working command on my OSX computer:

1. I created these directories:

         /Users/pdurand/tmp/beedeem/biobase  (1)
         /Users/pdurand/tmp/beedeem/log      (2)

           (1) will host my banks on my computer
           (2) will host BeeDeeM log files on my computer

2. Then I can install a bank as follows:

         docker run --name beedeem_machine -i -t --rm \
                    -v /Users/pdurand/tmp/beedeem/biobase:/biobase \
                    -v /Users/pdurand/tmp/beedeem/log:/var/beedeem \
                    beedeem_machine \
                    -c install swiss

In that case, BeeDeeM installs bank within directory '/biobase', which actually targets '/Users/pdurand/tmp/beedeem/biobase' through the Docker container. In a similar way, BeeDeeM creates a log file within '/var/beedeem', which is actually '/Users/pdurand/tmp/beedeem/log'.

## Test the container

Try a "docker run" using "-c install swiss":

        docker run --name beedeem_machine -i -t --rm \
        -v /Users/pdurand/tmp/beedeem/biobase:/biobase \
        -v /Users/pdurand/tmp/beedeem/log:/var/beedeem \
        beedeem_machine -c install swiss

*Important notice:* in the above command, dapt values of '-v' directives to target **YOUR** local directories.

After having installed that bank, you can try to query the bank as follows:

        docker run --name beedeem_machine -i -t --rm \
        -v /Users/pdurand/tmp/beedeem/biobase:/biobase \
        -v /Users/pdurand/tmp/beedeem/log:/var/beedeem \
        beedeem_machine -c query protein 1433S_HUMAN txt

*Important notice:* in the above command, dapt values of '-v' directives to target **YOUR** local directories.

## Additional notes
 
### Root access inside the container

First of all, comment out "ENTRYPOINT" at the end of the Dockerfile, then run a "docker build". 

Now, you'll be able to enter into the container, as follows:

     - if running: docker exec -it beedeem_machine bash

     - if not yet running: docker run --rm -i -t beedeem_machine bash
