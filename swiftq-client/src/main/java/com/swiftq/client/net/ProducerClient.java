package com.swiftq.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftq.common.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ProducerClient {

    private final String host;
    private final int port;
    private Channel channel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final EventLoopGroup group = new NioEventLoopGroup();

    // 用于保存请求与回调的映射，支持多请求异步返回
    private final ConcurrentHashMap<Long, CompletableFuture<Boolean>> pendingFutures = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);

    public ProducerClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                 .channel(NioSocketChannel.class)
                 .handler(new ChannelInitializer<Channel>() {
                     @Override
                     protected void initChannel(Channel ch) {
                         ch.pipeline().addLast(new ProducerClientHandler());
                     }
                 });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }

    public CompletableFuture<Boolean> send(Message message) throws Exception {
        long requestId = requestIdGen.incrementAndGet();

        Request req = new Request();
        req.setType("publish");
        req.setMessage(message);
        req.setRequestId(requestId);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingFutures.put(requestId, future);

        byte[] jsonBytes = mapper.writeValueAsBytes(req);
        ByteBuf buf = Unpooled.wrappedBuffer(jsonBytes);
        channel.writeAndFlush(buf);

        return future;
    }

    public void close() {
        group.shutdownGracefully();
    }

    // 内部消息格式
    static class Request {
        private String type;
        private Message message;
        private long requestId;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public long getRequestId() { return requestId; }
        public void setRequestId(long requestId) { this.requestId = requestId; }
    }

    static class Response {
        private String status;
        private String error;
        private long requestId;
        private Object message; // 添加 message 字段以兼容服务器响应

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getRequestId() { return requestId; }
        public void setRequestId(long requestId) { this.requestId = requestId; }
        public Object getMessage() { return message; }
        public void setMessage(Object message) { this.message = message; }
    }

    class ProducerClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            String json = msg.toString(StandardCharsets.UTF_8);
            Response resp = mapper.readValue(json, Response.class);
            CompletableFuture<Boolean> future = pendingFutures.remove(resp.getRequestId());

            if (future != null) {
                if ("ok".equals(resp.getStatus())) {
                    future.complete(true);
                } else {
                    future.completeExceptionally(new RuntimeException(resp.getError()));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
