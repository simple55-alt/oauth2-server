package cn.ltysyn.inmusic.config;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AccessFilter implements GlobalFilter, Ordered {
	
	// url匹配器
	private AntPathMatcher pathMatcher = new AntPathMatcher();
	
	@Autowired
	private StringRedisTemplate redisTemplate;

	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return -500;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// TODO Auto-generated method stub
		
		String accessToken = extractToken(exchange.getRequest());
		
		String path = exchange.getRequest().getPath().value();
		
		if (pathMatcher.match("/**/v2/api-docs/**", path)) {
			return chain.filter(exchange);
		}
		
		if (pathMatcher.match("/service-center/user/registry", path)) {
			return chain.filter(exchange);
		}
		
		if (!pathMatcher.match("/auth-server/**", path)) {
			if (accessToken == null) {
				exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
				return exchange.getResponse().setComplete();
			} else {
				try {
					String token = redisTemplate.opsForValue().get("access:" + accessToken);
					if (token.isEmpty()) {
						exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
						return exchange.getResponse().setComplete();
					}
				} catch (Exception e) {
					// TODO: handle exception
					exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
					return exchange.getResponse().setComplete();
				}
			}
		}
		
		return chain.filter(exchange);

	}
	
	protected String extractToken(ServerHttpRequest	request) {
		List<String> strings = request.getHeaders().get("Authorization");
		String authToken = null;
		if (strings != null) {
			authToken = strings.get(0).substring("Bearer".length()).trim();
		}
		
		if (StringUtils.isBlank(authToken)) {
			strings = request.getQueryParams().get("access_token");
			if (strings != null) {
				authToken = strings.get(0);
			}
		}
		
		return authToken;
	}
	
	

}
