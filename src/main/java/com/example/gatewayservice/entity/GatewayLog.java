package com.example.gatewayservice.entity;

import lombok.Data;

import java.util.Date;

@Data
public class GatewayLog {
    /**访问实例*/
    private String targetServer;
    /**客户端类型**/
    private String clientType;
    /**请求路径*/
    private String requestPath;
    /**请求方法*/
    private String requestMethod;
    /**请求id**/
    private String requestId;
    /**协议 */
    private String schema;
    /**用户id**/
    private Long userId;
    /**用户账号**/
    private String username;
    /**请求体*/
    private String requestBody;
    /**响应体*/
    private String responseData;
    /**返回状态**/
    private Integer status;
    /**请求ip*/
    private String ip;
    /**请求时间*/
    private Date requestTime;
    /**响应时间*/
    private Date responseTime;
    /**执行时间*/
    private long executeTime;
    /**客户端标识**/
    private String clientId;
}
