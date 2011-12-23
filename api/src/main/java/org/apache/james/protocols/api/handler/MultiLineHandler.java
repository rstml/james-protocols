package org.apache.james.protocols.api.handler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.ProtocolSession.State;
import org.apache.james.protocols.api.Response;

/**
 * A special {@link LineHandler} which will "buffer" the received lines till a point and the push them all at
 * one to the {@link #onLines(ProtocolSession, Collection)} method
 * 
 *
 * @param <S>
 */
public abstract class MultiLineHandler<S extends ProtocolSession> implements LineHandler<S>{

    private static final String BUFFERED_LINES = "BUFFERED_LINES";
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.handler.LineHandler#onLine(org.apache.james.protocols.api.ProtocolSession, byte[])
     */
    @SuppressWarnings("unchecked")
    public Response onLine(S session, ByteBuffer line) {
        Collection<ByteBuffer> lines = (List<ByteBuffer>) session.getAttachment(BUFFERED_LINES, State.Transaction);
        if (lines == null)  {
            lines = new ArrayList<ByteBuffer>();
            session.setAttachment(BUFFERED_LINES, lines, State.Transaction);
        }
        lines.add(line);
        if (isReady(session, line)) {
            return onLines(session, (Collection<ByteBuffer>) session.setAttachment(BUFFERED_LINES, null, State.Transaction));
        }
        return null;
    }

    /**
     * Return <code>true</code> if the buffered lines are ready to get pushed to the {@link #onLines(ProtocolSession, Collection)} method
     * 
     * @param session
     * @param line
     * @return ready
     */
    protected abstract boolean isReady(S session, ByteBuffer line);
    
    /**
     * Handle the buffered lines
     * 
     * @param session
     * @param lines
     * @return response
     */
    protected abstract Response onLines(S session, Collection<ByteBuffer> lines);
}
