# Contains the database descriptors to process.

#Comma separated list of descriptor files describing what to download/Process.
#Do not put space characters in this list. All names listed here
#correspond to real files located in this directory, and having the
#extension '.properties'.
db.list=uniprotJustInProd

#The main task to execute. Must be one of the two following keys: 
#'info' or 'download'. Use 'info' to just retrieve the list of
#files to download/process. Use 'download' to actually donwload/process 
#files. When using 'info', the list of files (along with their size) is 
#dumpped in the log file of the software.
db.main.task=download

#Resume a previously aborted process. To do that, replace 'none' by
# the process date using the format yyyymmdd (ex: 20071027).
resume.date=none

#Delay (ms) between two consecutive task executions.
task.delay=1000

#Delay (ms) between two consecutive FTP connections.
ftp.delay=5000
#Maximum number of attempts to download a single file.
ftp.retry=3

#Mailer configuration. Leave values empty if no mailer available.
mail.smtp.host=
mail.smtp.port=
mail.smtp.sender.mail=
mail.smtp.sender.pswd=
mail.smtp.recipient.mail=
