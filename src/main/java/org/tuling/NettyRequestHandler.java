package org.tuling;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by btc on 2018/1/3.
 *
 * @author shining
 */
public class NettyRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRequestHandler.class);
    private HttpRequest request;
    private HttpHeaders headers;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            headers = request.headers();
            String uri = request.getUri();
            if (uri.equalsIgnoreCase(Constants.FAVICON_ICO)) {
                return;
            }
            processQueryString(request, uri);
            if (request.getMethod().equals(HttpMethod.GET)) {

            } else if (request.getMethod().equals(HttpMethod.POST)) {
                switch (getContentType()) {
                    case Constants.JSON:
                        String content = ((FullHttpRequest)request).content().toString(CharsetUtil.UTF_8);
                        JSONObject json = JSON.parseObject(content);
                        if (json != null){
                            json.entrySet().forEach(entry->{
                                  LOGGER.info("json param key:{},value:{}",entry.getKey(),entry.getValue());
                            });
                        }
                        break;
                    case Constants.FORM:
                        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE),request,CharsetUtil.UTF_8);
                        decoder.getBodyHttpDatas().stream().filter(data-> data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute).forEach(value->{
                            Attribute attr = (Attribute)value;
                            try {
                                LOGGER.info("form param,key:{},value:{}",attr.getName(),attr.getValue());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        break;
                    case Constants.MULTI_PART:
                        HttpPostMultipartRequestDecoder multipartRequestDecoder = new HttpPostMultipartRequestDecoder(new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE),request,CharsetUtil.UTF_8);
                        multipartRequestDecoder.getBodyHttpDatas().stream().filter(data -> data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload).forEach(value->{
                            FileUpload fileUpload = (FileUpload) value;
                            if (fileUpload.isCompleted()){
                                String fileName = fileUpload.getFilename();
                                try {
                                    fileUpload.renameTo(new File("E:\\"+fileName));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                LOGGER.info("upload {} finished",fileName);
                            }
                        });
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

            }
            writeHttpResponse(ctx);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void writeHttpResponse(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.copiedBuffer("sample response",CharsetUtil.UTF_8));
        ChannelFuture future = ctx.channel().write(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private void processQueryString(HttpRequest request, String uri) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri, CharsetUtil.UTF_8);
        LOGGER.info("request uri:{}", uri);
        queryStringDecoder.parameters().forEach((key, value) -> {
            List<String> attrValue = value;
            attrValue.forEach(listValue -> {
                LOGGER.info("query param key:{},value:{}", key, listValue);
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.info(cause.getMessage(), cause);
        ctx.close();

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private String getContentType() {
        String contentType = headers.get(Constants.CONTENT_TYPE).split(":")[0];
        if (contentType.contains(";")) {
            return contentType.substring(0, contentType.indexOf(";"));
        }
        return contentType;
    }
}
