package org.tuling;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by btc on 2018/1/3.
 */
public class HttpServer {
    private static final Logger LOGGER= LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) throws InterruptedException {
        new HttpServer().start();
    }

    private void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast("request-decoder",new HttpRequestDecoder());
                            channel.pipeline().addLast("aggregater",new HttpObjectAggregator(1024*1024));
                            channel.pipeline().addLast("response-encoder",new HttpResponseEncoder());
                            channel.pipeline().addLast("handler",new NettyRequestHandler());
                        }
                    }).option(ChannelOption.SO_BACKLOG,1024).childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childOption(ChannelOption.TCP_NODELAY,true);
            ChannelFuture future = serverBootstrap.bind(8000).sync();
            LOGGER.info("server started at port 8000");
            future.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
