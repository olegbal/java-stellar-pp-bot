package com.github.olegbal.javastellarppbot.bot;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Server;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class HorizonServerManager {
    private final List<Server> horizonServers;
    private final PriorityQueue<PriorityServer> prioritisedHorizonServerQueue;

    public HorizonServerManager(List<Server> horizonServers) {
        this.horizonServers = horizonServers;

        List<PriorityServer> initialPriorityServers = horizonServers.stream()
                .map(PriorityServer::new)
                .toList();
        initialPriorityServers.stream().findAny().get().order = 1;

        prioritisedHorizonServerQueue = new PriorityQueue<>(Comparator.reverseOrder());
        prioritisedHorizonServerQueue.addAll(initialPriorityServers);
    }

    public Server getRelevantServer() {
        PriorityServer relevantServer = prioritisedHorizonServerQueue.poll();
        relevantServer.order = relevantServer.order == Integer.MAX_VALUE? 1 : relevantServer.order + 1;
        prioritisedHorizonServerQueue.add(relevantServer);
        return relevantServer.server;
    }

    private static class PriorityServer implements Comparable<PriorityServer> {
        private int order;
        private final Server server;

        public PriorityServer(Server server) {
            this.server = server;
        }

        @Override
        public int compareTo(@NotNull PriorityServer o) {
            return Integer.compare(o.order, this.order);
        }
    }
}
