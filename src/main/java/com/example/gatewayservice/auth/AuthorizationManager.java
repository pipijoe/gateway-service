package com.example.gatewayservice.auth;

import cn.hutool.core.convert.Convert;
import com.example.gatewayservice.constant.AuthConstant;
import com.example.gatewayservice.constant.RedisConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权管理器，用于判断是否有资源的访问权限
 * @author Joetao
 * @date 2022/5/25
 */
@Component
@Slf4j
@AllArgsConstructor
public class AuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final RedisTemplate<String,Object> redisTemplate;
    private final TokenStore redisTokenStore;


    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> mono, AuthorizationContext authorizationContext) {
        // 对应跨域的预检请求直接放行
        if (authorizationContext.getExchange().getRequest().getMethod() == HttpMethod.OPTIONS) {
            return Mono.just(new AuthorizationDecision(true));
        }

        String authorizationToken = authorizationContext.getExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("当前请求头Authorization中的值:{}",authorizationToken);
        if (StringUtils.isBlank(authorizationToken)) {
            log.warn("当前请求头Authorization中的值不存在");
            return Mono.just(new AuthorizationDecision(false));
        }

        String token = authorizationToken.replace(OAuth2AccessToken.BEARER_TYPE + " ", "");
        OAuth2Authentication oAuth2Authentication = redisTokenStore.readAuthentication(token);
        String clientId = oAuth2Authentication.getOAuth2Request().getClientId();
        ServerHttpResponse response = authorizationContext.getExchange().getResponse();
        response.getHeaders().set("client_id", clientId);
        //通过客户端方式访问，则直接放行，不进行权限校验，由服务自身去校验
        if (oAuth2Authentication.isClientOnly()) {
            response.getHeaders().set("client_type", "client");
            return Mono.just(new AuthorizationDecision(true));
        } else {
            response.getHeaders().set("client_type", "password");
        }

        //从Redis中获取当前路径可访问角色列表
        URI uri = authorizationContext.getExchange().getRequest().getURI();
        Object obj = redisTemplate.opsForHash().get(RedisConstant.RESOURCE_ROLES_MAP, uri.getPath());
        List<String> authorities = Convert.toList(String.class,obj);
        authorities = authorities.stream().map(i -> i = AuthConstant.AUTHORITY_PREFIX + i).collect(Collectors.toList());
        //认证通过且角色匹配的用户可访问当前路径
        return mono
                .filter(Authentication::isAuthenticated)
                .flatMapIterable(Authentication::getAuthorities)
                .map(GrantedAuthority::getAuthority)
                .any(authorities::contains)
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));

    }
}
