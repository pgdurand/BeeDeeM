package bzh.plealog.dbmirror.util.conf;

/**
 * A Bank handler.
 * 
 * @author Patrick G. Durand
 * */
public interface DeleteBankHandler {
  
  /**
   * Method called just before deleting a personal bank.
   * 
   * @return true to confirm that a bank can be deleted. False to abort deleting process.
   */
  public boolean confirmPersonalBankDeletion();

  /**
   * Method called just before deleting a bank.
   * 
   * @param path path targeting the bank to be deleted
   * @param list of name of banks to be deleted
   * 
   * @return true to confirm that a bank can be deleted. False to abort deleting process.
   */
  public boolean confirmBankDeletion(String path, String deletedDbs);

  /**
   * Method called when a bank failed to be deleted. Implementation should report
   * a message.
   */
  public void cannotDeleteBank();
}
