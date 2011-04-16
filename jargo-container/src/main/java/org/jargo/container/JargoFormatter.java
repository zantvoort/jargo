/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Jargo - JSE Container Toolkit.
 * Copyright (C) 2006  Leon van Zantvoort
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 * 
 * Leon van Zantvoort
 * 243 Acalanes Drive #11
 * Sunnyvale, CA 94086
 * USA
 *
 * zantvoort@users.sourceforge.net
 * http://jargo.org
 */
package org.jargo.container;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Leon van Zantvoort
 */
public final class JargoFormatter extends Formatter {
    
    Date dat = new Date();
    private static final String format = "{0,date,short} {0,time,HH:mm:ss.SSS}";
    private MessageFormat formatter;
    
    private Object args[] = new Object[1];
    
    private String lineSeparator = System.getProperty("line.separator");
    
    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");
        String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(" ");
        if (false && record.getSourceClassName() != null) { // Disable source class logging for now.
            sb.append("[" + record.getSourceClassName() + "]");
            String loggerName = record.getLoggerName();
            if (loggerName != null && !loggerName.equals(
                    record.getSourceClassName())) {
                sb.append(" <" + loggerName + ">");
            }
        } else {
            sb.append("<" + record.getLoggerName() + ">");
        }
        if (record.getSourceMethodName() != null) {
            if (message.equals("RETURN") ||
                    message.equals("THROW") ||
                    message.startsWith("ENTRY")) {
                sb.append(" ");
                sb.append(record.getSourceMethodName());
            }
        }
        sb.append(" ");
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
