/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;


/**
 * {@code HandlerResultHandler} that handles return values from methods annotated
 * with {@code @ResponseBody} writing to the body of the request or response with
 * an {@link HttpMessageConverter}.
 *
 * <p>By default the order for the result handler is set to 100. It detects the
 * presence of an {@code @ResponseBody} annotation and should be ordered after
 * result handlers that look for a specific return type such as
 * {@code ResponseEntity}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class ResponseBodyResultHandler extends AbstractMessageConverterResultHandler
		implements HandlerResultHandler {

	/**
	 * Constructor with message converters and a {@code ConversionService} only
	 * and creating a {@link HeaderContentTypeResolver}, i.e. using Accept header
	 * to determine the requested content type.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService) {

		this(converters, conversionService, new HeaderContentTypeResolver());
	}

	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting other reactive types (e.g.
	 * rx.Observable, rx.Single, etc.) to Flux or Mono
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		super(converters, conversionService, contentTypeResolver);
		setOrder(100);
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			MethodParameter returnType = ((HandlerMethod) handler).getReturnType();
			Class<?> containingClass = returnType.getContainingClass();
			return (AnnotationUtils.findAnnotation(containingClass, ResponseBody.class) != null ||
					returnType.getMethodAnnotation(ResponseBody.class) != null);
		}
		return false;
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Object body = result.getReturnValue().orElse(null);
		ResolvableType bodyType = result.getReturnValueType();
		return writeBody(exchange, body, bodyType);
	}

}
