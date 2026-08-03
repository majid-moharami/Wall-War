package com.heroiclabs.nakama;

import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Bridge class in the same package as the Nakama SDK to access package-private 
 * classes and methods needed to fix the countMultiple bug.
 */
public class NakamaBridge {
    @SuppressWarnings("unchecked")
    public static ListenableFuture<MatchmakerTicket> addMatchmaker(
            SocketClient socket,
            int minCount,
            int maxCount,
            String query,
            Map<String, String> stringProperties,
            Map<String, Double> numericProperties,
            int countMultiple
    ) {
        if (!(socket instanceof WebSocketClient)) {
            throw new IllegalArgumentException("Socket must be an instance of WebSocketClient to use this bridge.");
        }
        
        WebSocketClient client = (WebSocketClient) socket;

        // Construct the message manually
        MatchmakerAddMessage msg = new MatchmakerAddMessage();
        msg.setMinCount(minCount);
        msg.setMaxCount(maxCount);
        msg.setQuery(query);
        msg.setStringProperties(stringProperties);
        msg.setNumericProperties(numericProperties);
        msg.setCountMultiple(countMultiple);

        // Wrap in envelope
        WebSocketEnvelope env = new WebSocketEnvelope();
        env.setMatchmakerAdd(msg);

        // Send using reflection because the 'send' method is private
        try {
            Method sendMethod = WebSocketClient.class.getDeclaredMethod("send", WebSocketEnvelope.class);
            sendMethod.setAccessible(true);
            return (ListenableFuture<MatchmakerTicket>) sendMethod.invoke(client, env);
        } catch (Exception e) {
            throw new RuntimeException("NakamaBridge failed to invoke private send method", e);
        }
    }
}
