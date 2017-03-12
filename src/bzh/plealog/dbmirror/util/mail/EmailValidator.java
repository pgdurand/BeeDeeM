/* Copyright (C) 2007-2017 Ludovic Antin
 *
 */
package bzh.plealog.dbmirror.util.mail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email vaildator.
 * 
 * @author Ludovic Antin
 */
public class EmailValidator {
  // From:
  // http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/
  private static Pattern      pattern;

  private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  static {
    pattern = Pattern.compile(EMAIL_PATTERN);
  }

  /**
   * Validate email with regular expression
   * 
   * @param email
   *          email for validation
   * @return true valid email, false invalid email
   */
  public static boolean validate(final String email) {
    Matcher matcher = pattern.matcher(email);
    return matcher.matches();
  }
}
