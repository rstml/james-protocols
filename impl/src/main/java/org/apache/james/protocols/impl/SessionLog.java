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

import org.apache.commons.logging.Log;

/**
 * A {@link Log} implementation which suffix every log message with the session id
 * Id
 * 
 * 
 */
public class SessionLog implements Log {
    private Log logger;
    private String id;

    public SessionLog(String id, Log logger) {
        this.logger = logger;
        this.id = id;
    }

    private String getText(Object obj) {
        return "ID="+ id + " " + obj.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#debug(java.lang.Object)
     */
    public void debug(Object arg0) {
        logger.debug(getText(arg0));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#debug(java.lang.Object, java.lang.Throwable)
     */
    public void debug(Object arg0, Throwable arg1) {
        logger.debug(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#error(java.lang.Object)
     */
    public void error(Object arg0) {
        logger.error(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public void error(Object arg0, Throwable arg1) {
        logger.error(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object)
     */
    public void fatal(Object arg0) {
        logger.fatal(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object, java.lang.Throwable)
     */
    public void fatal(Object arg0, Throwable arg1) {
        logger.fatal(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#info(java.lang.Object)
     */
    public void info(Object arg0) {
        logger.info(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#info(java.lang.Object, java.lang.Throwable)
     */
    public void info(Object arg0, Throwable arg1) {
        logger.info(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isFatalEnabled()
     */
    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isTraceEnabled()
     */
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#trace(java.lang.Object)
     */
    public void trace(Object arg0) {
        logger.trace(getText(arg0));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#trace(java.lang.Object, java.lang.Throwable)
     */
    public void trace(Object arg0, Throwable arg1) {
        logger.trace(getText(arg0), arg1);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#warn(java.lang.Object)
     */
    public void warn(Object arg0) {
        logger.warn(getText(arg0));

    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public void warn(Object arg0, Throwable arg1) {
        logger.warn(getText(arg0), arg1);

    }

}
