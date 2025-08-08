package com.swiftq.broker.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftq.common.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class BrokerServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final BlockingQueue<Message> queue;
    private final ObjectMapper mapper;

    public BrokerServerHandler(BlockingQueue<Message> queue, ObjectMapper mapper) {
        this.queue = queue;
        this.mapper = mapper;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String json = msg.toString(StandardCharsets.UTF_8);

        Request request;
        try {
            request = mapper.readValue(json, Request.class);
        } catch (Exception e) {
            sendError(ctx, "Invalid JSON", 0L);
            return;
        }

        switch (request.getType()) {
            case "publish":
                handlePublish(ctx, request.getMessage(), request.getRequestId());
                break;
            case "consume":
                handleConsume(ctx, request.getRequestId());
                break;
            default:
                sendError(ctx, "Unknown command", request.getRequestId());
        }
    }

    private void handlePublish(ChannelHandlerContext ctx, Message message, long requestId) throws Exception {
        if (message == null) {
            sendError(ctx, "Message is null", requestId);
            return;
        }
        queue.offer(message);
        Response resp = new Response("ok", null, null, requestId);
        sendResponse(ctx, resp);
    }

    private void handleConsume(ChannelHandlerContext ctx, long requestId) throws Exception {
        Message message = queue.poll();
        if (message == null) {
            Response resp = new Response("empty", null, null, requestId);
            sendResponse(ctx, resp);
        } else {
            Response resp = new Response("ok", message, null, requestId);
            sendResponse(ctx, resp);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, Response resp) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(resp);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        ctx.writeAndFlush(buf);
    }

    private void sendError(ChannelHandlerContext ctx, String errorMsg, long requestId) throws Exception {
        Response resp = new Response("error", null, errorMsg, requestId);
        sendResponse(ctx, resp);
    }

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
        private Message message;
        private String error;
        private long requestId;

        public Response() {}

        public Response(String status, Message message, String error, long requestId) {
            this.status = status;
            this.message = message;
            this.error = error;
            this.requestId = requestId;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getRequestId() { return requestId; }
        public void setRequestId(long requestId) { this.requestId = requestId; }
    }
}
