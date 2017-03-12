/* Copyright (C) 2007-2017 Patrick G. Durand
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/agpl-3.0.txt
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 */
package bzh.plealog.dbmirror.util.ant;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * This class is used to execute Ant targets. Adapted from
 * http://marc.info/?l=ant-user&m=109389086707382&w=2.
 */
public class PAntRunner {
  Project _project;

  /**
   * Initializes a new Ant Project.
   * 
   * @param buildFile
   *          The build File to use. If none is provided, it will be defaulted
   *          to "build.xml".
   * @param baseDir
   *          The project's base directory. If none is provided, will be
   *          defaulted to "." (the current directory).
   * @throws Exception
   *           Exceptions are self-explanatory (read their Message)
   */
  public void init(String buildFile, String baseDir) throws Exception {
    // Create a new project, and perform some default initialization
    _project = new Project();
    try {
      _project.init();
      // _project.addBuildListener(new KLAntBuildListener());
    } catch (BuildException e) {
      throw new Exception("The default task list could not be loaded.");
    }

    // Set the base directory. If none is given, "." is used.
    if (baseDir == null)
      baseDir = new String(".");
    try {
      _project.setBasedir(baseDir);
    } catch (BuildException e) {
      throw new Exception(
          "The given basedir doesn't exist, or isn't a directory.");
    }

    // Parse the given buildfile. If none is given, "build.xml" is used.
    if (buildFile == null)
      buildFile = new String("build.xml");
    try {
      ProjectHelper.getProjectHelper().parse(_project, new File(buildFile));
    } catch (BuildException e) {
      throw new Exception("Configuration file " + buildFile
          + " is invalid, or cannot be read.");
    }
  }

  /**
   * Sets the project's properties. May be called to set project-wide
   * properties, or just before a target call to set target-related properties
   * only.
   * 
   * @param properties
   *          A map containing the properties' name/value couples
   * @param overridable
   *          If set, the provided properties values may be overriden by the
   *          config file's values
   * @throws Exception
   *           Exceptions are self-explanatory (read their Message)
   */
  public void setProperties(Map<String, String> properties, boolean overridable)
      throws Exception {
    // Test if the project exists
    if (_project == null)
      throw new Exception(
          "Properties cannot be set because the project has not been initialized. Please call the 'init' method first !");

    // Property hashmap is null
    if (properties == null)
      throw new Exception("The provided property map is null.");

    // Loop through the property map
    Set<String> propertyNames = properties.keySet();
    Iterator<String> iter = propertyNames.iterator();
    while (iter.hasNext()) {
      // Get the property's name and value
      String propertyName = (String) iter.next();
      String propertyValue = (String) properties.get(propertyName);
      if (propertyValue == null)
        continue;

      // Set the properties
      if (overridable)
        _project.setProperty(propertyName, propertyValue);
      else
        _project.setUserProperty(propertyName, propertyValue);
    }
  }

  /**
   * Runs the given Target.
   * 
   * @param target
   *          The name of the target to run. If null, the project's default
   *          target will be used.
   * @throws Exception
   *           Exceptions are self-explanatory (read their Message)
   */
  public void runTarget(String target) throws Exception {
    // Test if the project exists
    if (_project == null)
      throw new Exception(
          "No target can be launched because the project has not been initialized. Please call the init' method first !");

    // If no target is specified, run the default one.
    if (target == null)
      target = _project.getDefaultTarget();

    // Run the target
    try {
      _project.executeTarget(target);
    } catch (BuildException e) {
      throw new Exception(e.getMessage());
    }
  }
}
