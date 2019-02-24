package com.kodewerk.safepoint.web;

import com.kodewerk.safepoint.event.JVMEvent;
import com.kodewerk.safepoint.event.Safepoint;
import com.kodewerk.safepoint.event.SafepointCause;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebView extends AbstractVerticle {

    private final static Logger LOG = Logger.getLogger(WebView.class.getName());

    private Map<JVMEvent, Integer> causes = new HashMap<>();

    @Override
    public void start(Future<Void> future) {
        Router router = Router.router(vertx);

        vertx.eventBus().<JVMEvent>consumer("aggregator-inbox", msg -> {
            JVMEvent event = msg.body();
            causes.put(event, causes.getOrDefault(event, 0) + 1);
        });

        router.route("/events/*").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            final String content = buildContent(causes);
            response.putHeader("content-type", "text/html").end("<h1>Events</h1><br>" + content);

        });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        LOG.info("HTTP server running on port 8080");
                        future.complete();
                    } else {
                        LOG.severe(() -> "Could not start a HTTP server: " + result.cause());
                        future.fail(result.cause());
                    }
                });
    }

    private static String buildContent(Map<JVMEvent, Integer> causes) {
        return "<ul><li>"
                + causes.entrySet().stream()
                .map(e -> buildEvent(e.getKey()) + ": " + e.getValue())
                .collect(Collectors.joining("</li><li>"))
                + "</li></ul>";
    }

    private static String buildEvent(JVMEvent event) {
        return event.toString() + ", " + event.getDuration() + ", " + event.getEventTime();
    }


}
