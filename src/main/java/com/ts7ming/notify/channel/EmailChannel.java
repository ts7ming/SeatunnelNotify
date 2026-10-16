package com.ts7ming.notify.channel;

import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.util.List;
import java.util.Properties;

/**
 * 邮件通知渠道 (基于 JavaMail)。
 */
public class EmailChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);
    private final ChannelConfig cfg;

    public EmailChannel(ChannelConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public String type() {
        return "email";
    }

    @Override
    public void send(NotifyMessage msg) {
        String host = cfg.getString("host", "");
        int port = cfg.getInt("port", 25);
        String user = cfg.getString("username", "");
        String pass = cfg.getString("password", "");
        String from = cfg.getString("from", user);
        List<String> to = cfg.getList("to");
        if (Common.isBlank(host) || to.isEmpty()) {
            log.warn("邮件渠道配置不完整 (host/to), 跳过发送");
            return;
        }
        String subject = cfg.getString("subject", msg.getTitle());
        boolean ssl = cfg.getBool("ssl", true);
        String body = msg.getTitle() + "\n\n" + msg.getContent();

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.transport.protocol", "smtp");
            boolean auth = !Common.isBlank(user);
            props.put("mail.smtp.auth", auth ? "true" : "false");
            if (ssl) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session = auth
                    ? Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user, pass);
                        }
                    })
                    : Session.getInstance(props);

            MimeMessage m = new MimeMessage(session);
            m.setFrom(new InternetAddress(from));
            for (String t : to) {
                if (!Common.isBlank(t)) {
                    m.addRecipient(Message.RecipientType.TO, new InternetAddress(t.trim()));
                }
            }
            m.setSubject(subject, "UTF-8");
            m.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(body.getBytes("UTF-8"), "text/plain; charset=UTF-8")));
            m.saveChanges();

            Transport.send(m);
            log.info("邮件通知发送成功, 收件人: {}", to);
        } catch (Exception e) {
            log.error("邮件通知发送失败", e);
        }
    }
}
