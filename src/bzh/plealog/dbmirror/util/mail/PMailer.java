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
package bzh.plealog.dbmirror.util.mail;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This is a generic mailer system.
 * 
 * @author Patrick G. Durand
 */
public class PMailer {

  private String           _senderMail;
  private String           _smtpHost;
  private String           _password;
  private int              _smtpPort = 25;
  private boolean          _debug    = false;

  private static final Log LOGGER    = LogFactory
                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                             + ".PMailer");

  private class SMTPAuthenticator extends Authenticator {
    public PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(_senderMail, _password);
    }
  }

  @SuppressWarnings("unused")
  private PMailer() {
  }

  public PMailer(String host, String sender) {
    setSmtpHost(host);
    setSenderMail(sender);
  }

  private void setFileAsAttachment(Message msg, String body, String filename)
      throws MessagingException {
    Multipart mp;
    MimeBodyPart p1, p2;
    FileDataSource fds;

    p1 = new MimeBodyPart();
    p1.setText(body);

    p2 = new MimeBodyPart();

    fds = new FileDataSource(filename);
    p2.setDataHandler(new DataHandler(fds));
    p2.setFileName(fds.getName());

    mp = new MimeMultipart();
    mp.addBodyPart(p1);
    mp.addBodyPart(p2);

    msg.setContent(mp);
  }

  private void setTextContent(Message msg, String body)
      throws MessagingException {
    msg.setContent(body, "text/plain");
  }

  private Message prepareMail(String[] recipients, String subject) {
    Properties props;
    Authenticator auth;
    Session session;
    Message msg;
    InternetAddress[] addresses;
    InternetAddress sender;
    int i, nbReceipients;

    try {
      props = new Properties();
      props.put("mail.smtp.host", _smtpHost);
      props.put("mail.smtp.port", String.valueOf(_smtpPort));
      if (!_debug)
        props.put("mail.debug", "false");// just in case we need to see what
                                         // happens
      else
        props.put("mail.debug", "true");
      if (_password != null) {
        props.put("mail.smtp.auth", "true");
        auth = new SMTPAuthenticator();
        session = Session.getDefaultInstance(props, auth);
      } else {
        session = Session.getDefaultInstance(props);
      }
      msg = new MimeMessage(session);
      sender = new InternetAddress(_senderMail);
      msg.setFrom(sender);
      nbReceipients = recipients.length;

      if (nbReceipients == 1) {
        addresses = new InternetAddress[1];
        addresses[0] = new InternetAddress(recipients[0]);
      } else {
        addresses = new InternetAddress[nbReceipients];
        for (i = 0; i < recipients.length; i++) {
          addresses[i] = new InternetAddress(recipients[i]);
        }
      }
      msg.setRecipients(Message.RecipientType.TO, addresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
    } catch (Exception ex) {
      LOGGER.error("Error while preparing email: " + ex);
      msg = null;
    }
    return msg;
  }

  public boolean sendMail(String[] to, String subject, String body) {
    Message msg;
    boolean bRet = false;

    if (to == null || to.length == 0) {
      LOGGER.error("Unable to send email: no recipients");
      return false;
    }
    if (subject == null || subject.length() == 0) {
      LOGGER.error("Unable to send email: no subject");
      return false;
    }
    if (body == null || body.length() == 0) {
      LOGGER.error("Unable to send email: no body");
      return false;
    }
    try {
      msg = prepareMail(to, subject);
      if (msg != null) {
        setTextContent(msg, body);
        Transport.send(msg);
        bRet = true;
      }
    } catch (Exception ex) {
      LOGGER.error("Unable to send email: " + ex);
    }
    return bRet;

  }

  public boolean sendMail(String to, String subject, String body) {
    if (to.indexOf(',') != -1)
      return this.sendMail(Utils.tokenize(to), subject, body);
    else
      return this.sendMail(new String[] { to }, subject, body);
  }

  public boolean sendMail(String[] to, String subject, String body,
      String attachment) {
    File f;
    Message msg;
    boolean bRet = false;

    if (to == null || to.length == 0) {
      LOGGER.error("Unable to send email: no recipients");
      return false;
    }
    if (subject == null || subject.length() == 0) {
      LOGGER.error("Unable to send email: no subject");
      return false;
    }
    if (body == null || body.length() == 0) {
      LOGGER.error("Unable to send email: no body");
      return false;
    }
    if (attachment == null || attachment.length() == 0) {
      LOGGER.error("Unable to send email: no attachment");
      return false;
    }
    try {
      f = new File(attachment);
      if (f.exists() == false) {
        throw new Exception("Attachment file not found: " + attachment);
      }
      msg = prepareMail(to, subject);
      if (msg != null) {
        setFileAsAttachment(msg, body, attachment);
        Transport.send(msg);
        bRet = true;
      }
    } catch (Exception ex) {
      LOGGER.error("Unable to send email: " + ex);
    }
    return bRet;
  }

  public boolean sendMail(String to, String subject, String body,
      String attachment) {
    if (to.indexOf(',') != -1)
      return this.sendMail(Utils.tokenize(to), subject, body, attachment);
    else
      return this.sendMail(new String[] { to }, subject, body, attachment);
  }

  public String getPassword() {
    return _password;
  }

  public void setPassword(String password) {
    this._password = password;
  }

  public String getSenderMail() {
    return _senderMail;
  }

  public void setSenderMail(String mail) {
    _senderMail = mail;
  }

  public String getSmtpHost() {
    return _smtpHost;
  }

  public void setSmtpHost(String host) {
    _smtpHost = host;
  }

  public int getSmtpPort() {
    return _smtpPort;
  }

  public void setSmtpPort(int port) {
    _smtpPort = port;
  }

  public void setDebug(boolean debug) {
    _debug = debug;
  }
}
