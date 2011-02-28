/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.protocols.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * A {@link Logger} implementation which suffix every log message with the session id
 * Id
 * 
 * 
 */
public class SessionLog implements Logger {
    private Logger logger;
    private String id;

    public SessionLog(String id, Logger logger) {
        this.logger = logger;
        this.id = id;
    }

    private String getText(String str) {
        return "ID="+ id + " " + str;
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#debug(java.lang.String)
     */
    public void debug(String arg0) {
        logger.debug(getText(arg0));
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#debug(java.lang.String, java.lang.StringThrowable)
     */
    public void debug(String arg0, Throwable arg1) {
        logger.debug(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#error(java.lang.String)
     */
    public void error(String arg0) {
        logger.error(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)
     */
    public void error(String arg0, Throwable arg1) {
        logger.error(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    public void info(String arg0) {
        logger.info(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable)
     */
    public void info(String arg0, Throwable arg1) {
        logger.info(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#isErrorEnabled()
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#isInfoEnabled()
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#isTraceEnabled()
     */
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#isWarnEnabled()
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#trace(java.lang.String)
     */
    public void trace(String arg0) {
        logger.trace(getText(arg0));
    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable)
     */
    public void trace(String arg0, Throwable arg1) {
        logger.trace(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#warn(java.lang.String)
     */
    public void warn(String arg0) {
        logger.warn(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable)
     */
    public void warn(String arg0, Throwable arg1) {
        logger.warn(getText(arg0), arg1);

    }

    public String getName() {
	return logger.getName();
    }

    public void trace(String format, Object arg) {
	logger.trace(format, arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
	logger.trace(format, arg1, arg2);
    }

    public void trace(String format, Object[] argArray) {
	logger.trace(format, argArray);
    }

    public boolean isTraceEnabled(Marker marker) {
	return logger.isTraceEnabled(marker);
    }

    public void trace(Marker marker, String msg) {
	logger.trace(marker, msg);
    }

    public void trace(Marker marker, String format, Object arg) {
	logger.trace(marker, format, arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
	logger.trace(marker, format, arg1, arg2);
    }

    public void trace(Marker marker, String format, Object[] argArray) {
	logger.trace(marker, format, argArray);
    }

    public void trace(Marker marker, String msg, Throwable t) {
	logger.trace(marker, msg, t);
    }

    public void debug(String format, Object arg) {
	logger.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
	logger.debug(format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
	logger.debug(format, argArray);
    }

    public boolean isDebugEnabled(Marker marker) {
	return logger.isDebugEnabled(marker);
    }

    public void debug(Marker marker, String msg) {
	logger.debug(marker, msg);
    }

    public void debug(Marker marker, String format, Object arg) {
	logger.debug(marker, format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
	logger.debug(marker, format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object[] argArray) {
	logger.debug(marker, format, argArray);
    }

    public void debug(Marker marker, String msg, Throwable t) {
	logger.debug(marker, msg, t);
    }

    public void info(String format, Object arg) {
	logger.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
	logger.info(format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
	logger.info(format, argArray);
    }

    public boolean isInfoEnabled(Marker marker) {
	return logger.isInfoEnabled(marker);
    }

    public void info(Marker marker, String msg) {
	logger.info(marker, msg);
    }

    public void info(Marker marker, String format, Object arg) {
	logger.info(marker, format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
	logger.info(marker, format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object[] argArray) {
	logger.info(marker, format, argArray);
    }

    public void info(Marker marker, String msg, Throwable t) {
	logger.info(marker, msg, t);
    }

    public void warn(String format, Object arg) {
	logger.warn(format, arg);
    }

    public void warn(String format, Object[] argArray) {
	logger.warn(format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
	logger.warn(format, arg1, arg2);
    }

    public boolean isWarnEnabled(Marker marker) {
	return logger.isWarnEnabled(marker);
    }

    public void warn(Marker marker, String msg) {
	logger.warn(marker, msg);
    }

    public void warn(Marker marker, String format, Object arg) {
	logger.warn(marker, format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
	logger.warn(marker, format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object[] argArray) {
	logger.warn(marker, format, argArray);
    }

    public void warn(Marker marker, String msg, Throwable t) {
	logger.warn(marker, msg, t);
    }

    public void error(String format, Object arg) {
	logger.error(format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
	logger.error(format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
	logger.error(format, argArray);
    }

    public boolean isErrorEnabled(Marker marker) {
	return logger.isErrorEnabled(marker);
    }

    public void error(Marker marker, String msg) {
	logger.error(marker, msg);
    }

    public void error(Marker marker, String format, Object arg) {
	logger.error(marker, format, arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
	logger.error(marker, format, arg1, arg2);
    }

    public void error(Marker marker, String format, Object[] argArray) {
	logger.error(marker, format, argArray);
    }

    public void error(Marker marker, String msg, Throwable t) {
	logger.error(marker, msg, t);
    }

}
