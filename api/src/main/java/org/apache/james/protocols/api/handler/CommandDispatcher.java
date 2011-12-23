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

package org.apache.james.protocols.api.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.james.protocols.api.BaseRequest;
import org.apache.james.protocols.api.FutureResponse;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;



/**
 *  A CommandDispatcher is responsible to call the right {@link CommandHandler} for a given Command
 *
 */
public class CommandDispatcher<Session extends ProtocolSession> implements ExtensibleHandler, LineHandler<Session> {
    /**
     * The list of available command handlers
     */
    private final HashMap<String, List<CommandHandler<Session>>> commandHandlerMap = new HashMap<String, List<CommandHandler<Session>>>();

    private final List<ProtocolHandlerResultHandler<Response, Session>> rHandlers = new ArrayList<ProtocolHandlerResultHandler<Response, Session>>();

    private final Collection<String> mandatoryCommands;
    
    public CommandDispatcher(Collection<String> mandatoryCommands) {
        this.mandatoryCommands = mandatoryCommands;
    }
    
    public CommandDispatcher() {
        this(Collections.<String>emptyList());
    }
    
    /**
     * Add it to map (key as command name, value is an array list of CommandHandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The CommandHandler object
     */
    protected void addToMap(String commandName, CommandHandler<Session> cmdHandler) {
        List<CommandHandler<Session>> handlers = commandHandlerMap.get(commandName);
        if(handlers == null) {
            handlers = new ArrayList<CommandHandler<Session>>();
            commandHandlerMap.put(commandName, handlers);
        }
        handlers.add(cmdHandler);
    }


    /**
     * Returns all the configured CommandHandlers for the specified command
     *
     * @param command the command name which will be key
     * @param session not null
     * @return List of CommandHandlers
     */
    protected List<CommandHandler<Session>> getCommandHandlers(String command, ProtocolSession session) {
        if (command == null) {
            return null;
        }
        if (session.getLogger().isDebugEnabled()) {
            session.getLogger().debug("Lookup command handler for command: " + command);
        }
        List<CommandHandler<Session>> handlers =  commandHandlerMap.get(command);
        if(handlers == null) {
            handlers = commandHandlerMap.get(getUnknownCommandHandlerIdentifier());
        }

        return handlers;
    }

    /**
     * @throws WiringException 
     * @see org.apache.james.protocols.api.handler.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (interfaceName.equals(ProtocolHandlerResultHandler.class)) {
            rHandlers.addAll(extension);
        }
        if (interfaceName.equals(CommandHandler.class)) {
            for (Iterator it = extension.iterator(); it.hasNext();) {
                CommandHandler handler = (CommandHandler) it.next();
                Collection implCmds = handler.getImplCommands();

                for (Iterator i = implCmds.iterator(); i.hasNext();) {
                    String commandName = ((String) i.next()).trim().toUpperCase(Locale.US);
                    addToMap(commandName, (CommandHandler) handler);
                }
            }
            
            if (commandHandlerMap.size() < 1) {
                throw new WiringException("No commandhandlers configured");
            } else {
                for (String cmd: mandatoryCommands) {
                    if (!commandHandlerMap.containsKey(cmd)) {
                        throw new WiringException("No commandhandlers configured for mandatory command " + cmd);
                    }
                }
            }
        }

    }
    
    /**
     * @see org.apache.james.protocols.api.handler.LineHandler#onLine(ProtocolSession, byte[])
     */
    public Response onLine(final Session session, byte[] line) {
        
        try {
            
            Request request = parseRequest(session, line);
            if (session.getLogger().isDebugEnabled()) {
                session.getLogger().debug(getClass().getName() + " received: " + request.getCommand());
            }
            List<CommandHandler<Session>> commandHandlers = getCommandHandlers(request.getCommand(), session);
            // fetch the command handlers registered to the command

            Iterator<CommandHandler<Session>> handlers = commandHandlers.iterator();
            
            while (handlers.hasNext()) {
                final long start = System.currentTimeMillis();
                CommandHandler<Session> cHandler = handlers.next();
                Response response = cHandler.onCommand(session, request);
                if (response != null) {
                    long executionTime = System.currentTimeMillis() - start;

                    // now process the result handlers
                    for (int a = 0; a < rHandlers.size(); a++) {
                        // Disable till PROTOCOLS-37 is implemented
                        if (response instanceof FutureResponse) {
                            session.getLogger().debug("ProtocolHandlerResultHandler are not supported for FutureResponse yet");
                            break;
                        } 
                        response = rHandlers.get(a).onResponse(session, response, executionTime, (CommandHandler<Session>) cHandler);
                    }
                }
                if (response != null) {
                    return response;
                }

            }
            return null;
        } catch (Exception e) {
            session.getLogger().debug("Unable to parse request", e);
            return session.newFatalErrorResponse();
        } 

       
    }

    /**
     * Parse the line into a {@link Request}
     * 
     * @param session
     * @param line
     * @return request
     * @throws Exception
     */
    protected Request parseRequest(Session session, byte[] line) throws Exception {
        String curCommandName = null;
        String curCommandArgument = null;
        String cmdString = new String(line, getLineDecodingCharset()).trim();
        int spaceIndex = cmdString.indexOf(" ");
        if (spaceIndex > 0) {
            curCommandName = cmdString.substring(0, spaceIndex);
            curCommandArgument = cmdString.substring(spaceIndex + 1);
        } else {
            curCommandName = cmdString;
        }
        curCommandName = curCommandName.toUpperCase(Locale.US);

        if (session.getLogger().isDebugEnabled()) {
            session.getLogger().debug(getClass().getName() + " received: " + cmdString);
        }

        BaseRequest request = new BaseRequest(curCommandName, curCommandArgument);
        return request;

    }
    
    
    protected String getLineDecodingCharset() {
        return "US-ASCII";
    }

    /**
     * @see org.apache.james.protocols.api.handler.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Class<?>> getMarkerInterfaces() {
        List res = new LinkedList();
        res.add(CommandHandler.class);
        res.add(ProtocolHandlerResultHandler.class);
        return res;
    }

    /**
     * Return the identifier to lookup the UnknownCmdHandler in the handler map
     * 
     * @return identifier
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCommandHandler.COMMAND_IDENTIFIER;
    }
}
