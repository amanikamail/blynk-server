package cc.blynk.server.core.application;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.handlers.common.ClientChannelStateHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import javax.net.ssl.SSLEngine;

/**
 * Application processing pipeline initializer.
 *
* The Blynk Project.
* Created by Dmitriy Dumanskiy.
* Created on 11.03.15.
*/
final class AppChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final SessionsHolder sessionsHolder;
    private final GlobalStats stats;
    private final AppHandlersHolder handlersHolder;
    private final SslContext sslCtx;
    private final int appTimeoutSecs;
    private final boolean isMutualSSL;

    public AppChannelInitializer(SessionsHolder sessionsHolder, GlobalStats stats, AppHandlersHolder handlersHolder, SslContext sslCtx, int appTimeoutSecs, boolean isMutualSSL) {
        this.sessionsHolder = sessionsHolder;
        this.stats = stats;
        this.handlersHolder = handlersHolder;
        this.sslCtx = sslCtx;
        this.appTimeoutSecs = appTimeoutSecs;
        this.isMutualSSL = isMutualSSL;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new ReadTimeoutHandler(appTimeoutSecs));

        SSLEngine engine = sslCtx.newEngine(ch.alloc());
        if (isMutualSSL) {
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(true);
        }
        pipeline.addLast(new SslHandler(engine));

        //non-sharable handlers
        pipeline.addLast(new ClientChannelStateHandler(sessionsHolder));
        pipeline.addLast(new MessageDecoder(stats));
        pipeline.addLast(new MessageEncoder());

        //sharable business logic handlers initialized previously
        for (ChannelHandler handler : handlersHolder.getAllHandlers()) {
            pipeline.addLast(handler);
        }
    }
}
