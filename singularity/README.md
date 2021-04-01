# Recipe to build a Singularity image for BeeDeeM software

How to build?
-------------
 
    singularity build -f beedeem.sif beedeem.def

How to configure?
-----------------

By default you have nothing to do but binding paths called /beedeem-wk and 
/beedeem-db (see next section, "How to run?").

In such a case, BeeDeeM will use its internal "conf" directory to locate
bank descriptors. So, if you install banks for which descriptors (.dsc files)
already exist in standard "BeeDeeM/conf/descriptor"s directory (e.g. 
Swissprot_human) then you can go to next section "How to run?".

Otherwise, if you want to use your own bank descriptors, proceed as follows.

> Option 1... the simple one. Simply setup your bank descriptor file (.dsc)
   outside the container. Then use "install.sh" tool of BeeDeeM and pass to its
   "-dsc" argument the full path to that file. Of course, use "--bind" Singularity
   argument to enable BeeDeeM to access your file from inside the container.

 > Option 2... the less simple one. Start by creating a "conf" directory, directly
   from BeeDeeM project, as follows:

     cd /some/path/on/your/machine
     git clone https://github.com/pgdurand/BeeDeeM.git
     (conf directory is: ./BeeDeeM/conf)

   Then, before calling BeeDeeM tools using the container, setup that variable:

     export KL_CONF_DIR=/beedeem-conf

   Finally, at runtime (see below), bind paths as follows:
 
     --bind /some/path/on/your/machine/BeeDeeM/conf:/beedeem-conf 
 

 How to run?
 -----------

 BeeDeeM (inside Singularity container) expects to find 2 directories for its 
  own usage, as follows:

     /beedeem-wk  : BeeDeeM working (tmp) directory
     /beedeem-db  : place to install databanks loaded by BeeDeeM

  These folders are defined in the Singularity recipe... so, you just have to 
  use '--bind' argument accordingly to attach these mount points to appropriate
  folders on your host system.

  Calling a BeeDeeM tool command is then as easy as:
      
      install.sh -desc PDB_proteins
      (standard BeeDeeM command to install PDB_proteins bank)

  Here is a working example (adapt --bind to your host configuration):

    singularity run \
       --bind $SCRATCH/beedeem/bdm-wk:/beedeem-wk     # BeeDeeM working directory \
       --bind $DATAREF/beedeem/banks:/beedeem-db      # Where to install banks \
       beedeem.sif                                    # beedeem Singularity image\
       install.sh -desc PDB_proteins                  # install PDB_proteins bank (1)

     (1) targets container file /opt/beedeem/conf/descriptors/PDB_proteins.dsc

  Before calling above command, you may setup KL_JRE_ARGS to control resources used 
  by install.sh as follows:

    export KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=/beedeem-wk"

  In a similar way, let install.sh be silent (no log):

    export KL_LOG_TYPE=none

  ... or redirect everything to console (no log file, which is default mode):
 
    export KL_LOG_TYPE=console

  When using default log file mode, set YOUR log file name as follows:

    export KL_LOG_FILE=myfile.log
    (log file is always written in beedeem working directory).

  Finally, you can turn on DEBUG log mode as follows:

    export KL_DEBUG=true


  Using Aspera with BeeDeeM
  -------------------------

  Aspera tool cannot be distributed as part of BeeDeeM due to IBM license restrictions.
  Let us suppose you have installed Aspera in that directory on your host system:

    /opt/tools/aspera-linux 

  Before running BeeDeeM Singularity container, setup these two variables:

    export KL_aspera__bin__path=/software/aspera-linux/cli/bin/ascp
    export KL_aspera__key__path=/software/aspera-linux/cli/etc/asperaweb_id_dsa.openssh

  Then, add that "--bind" argument in your singularity run command:

    --bind /opt/tools:/software

