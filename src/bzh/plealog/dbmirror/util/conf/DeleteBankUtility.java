package bzh.plealog.dbmirror.util.conf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.plealog.genericapp.api.EZEnvironment;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.event.DBMirrorEvent;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * Utility class for special configuration operations.
 * 
 * @author Patrick G. Durand
 */
public class DeleteBankUtility {
  
  /**
   * Return a string formatted list of bank to be deleted.
   * 
   * @param descriptors list of descriptors
   * @param path path targeting the bank to be deleted
   * @param osWin set to true if running on MS-Windows OS, false otherwise.
   * 
   * @return a string with the list of bank names to be deleted
   * */
  public static String getPotentialyDeletedBanks(List<IdxDescriptor> descriptors, String path, boolean osWin) {
    StringBuffer buf;
    IdxDescriptor d;
    String lPath, path2;
    int i, size;

    buf = new StringBuffer();
    // this was added because sometimes disk letter can be in lower or upper
    // case!
    if (osWin)
      lPath = path.toUpperCase();
    else
      lPath = path;
    size = descriptors.size();
    for (i = 0; i < size; i++) {
      d = (IdxDescriptor) descriptors.get(i);
      // when creating simultaneously data index and blast db, both types
      // of databank are in the same directory : when deleting one, actually
      // all the content of 'path' is removed. As a consequence, we have
      // to discard the other databank.
      path2 = d.getCode();
      if (osWin)
        path2 = path2.toUpperCase();
      if (path2.startsWith(lPath)) {
        File[] files = new File(lPath).listFiles();
        for (File f : files){
          if (f.isDirectory()){
            long diskSizeL = FileUtils.sizeOfDirectory(f);
            buf.append(d.getName());
            buf.append(File.separator);
            buf.append(f.getName());
            buf.append(" (");
            buf.append(Utils.getBytes(diskSizeL));
            buf.append(")");
            buf.append("\n");
          }
        }
      }
    }
    return buf.toString();
  }
  /**
   * Remove a bank from software repository.
   * 
   * @param descriptors list of descriptors
   * @param desc descriptor of the bank to delete
   * @param handler delete handler
   * 
   * @return true if bank deleted, false otherwise. return false if one of 
   * the parameters is null.
   */
  public static boolean deleteBank(List<IdxDescriptor> descriptors, IdxDescriptor desc, DeleteBankHandler handler) {
    ArrayList<IdxDescriptor> data;
    DBMirrorConfig mConfig;
    IdxDescriptor d;
    String path, path2, deletedDbs;
    boolean discard, osWin;
    int idx, i, size;
    List<String> deletedCodes = new ArrayList<String>();

    if (descriptors==null || descriptors.isEmpty() || desc == null || handler==null)
      return false;

    osWin = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS;
    discard = false;

    path = desc.getCode();
    idx = path.indexOf(DBMSAbstractConfig.CURRENT_DIR);
    if (idx != -1) {
      // mirror handled by KDMS
      path = path.substring(0, idx);
    } else {
      // personal DB : do not delete it, since it may be contained within a
      // directory
      // that also contains other databanks.
      path = null;
    }
    if (path != null) {
      // Physically remove bank(s) from disk
      deletedDbs = getPotentialyDeletedBanks(descriptors, path, osWin);
      if (handler.confirmBankDeletion(path, deletedDbs)==false){
        return false;
      }
      discard = true;
      EZEnvironment.setWaitCursor();
      if (!PAntTasks.deleteDirectory(path)) {
        DicoTermQuerySystem.closeDicoTermQuerySystem();
        if (!PAntTasks.deleteDirectory(path)) {
          handler.cannotDeleteBank();
          return false;
        }

      }
    }
    else{
      if (handler.confirmPersonalBankDeletion()==false){
        return false;
      }
    }
    //remove bank(s) from global config
    data = new ArrayList<IdxDescriptor>();
    size = descriptors.size();
    // this was added because sometimes disk letter can be in lower or upper
    // case!
    if (path !=null && osWin)
      path = path.toUpperCase();
    for (i = 0; i < size; i++) {
      d = (IdxDescriptor) descriptors.get(i);
      if (d != desc) {// compare by ref is ok here
        // when creating simultaneously data index and blast db, both types
        // of databank are in the same directory : when deleting one, actually
        // all the content of 'path' is removed. As a consequence, we have
        // to discard the other databank.
        path2 = d.getCode();
        if (osWin)
          path2 = path2.toUpperCase();
        if (discard) {
          if (path!=null && path2.startsWith(path) == false) {
            data.add(d);
          } else {
            deletedCodes.add(d.getKbCode());
          }
        } else {
          data.add(d);
        }
      } else {
        deletedCodes.add(d.getKbCode());
      }
    }
    mConfig = DBDescriptorUtils.getMirrorConfig(data, null);
    // store this deleted index to not be re-used
    mConfig.removeMirrorCode(deletedCodes);
    DBDescriptorUtils.saveDBMirrorConfig(
        DBMSAbstractConfig.getLocalMirrorConfFile(), mConfig);
    DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(mConfig,
        DBMirrorEvent.TYPE.dbRemoved));
    return true;
  }

}
