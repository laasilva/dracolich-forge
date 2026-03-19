package dm.dracolich.forge.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Wraps all @ResponseBody returns in a DmdResponse automatically.
 * <p>
 * Order is set to 50, which sits between ResponseEntityResultHandler (0)
 * and the default ResponseBodyResultHandler (100). This way:
 * - ResponseEntity returns (e.g. from ControllerAdvice exception handlers) are handled first at order 0
 * - Our wrapper picks up plain Mono/Flux returns from controllers at order 50
 * - The default handler at order 100 is never reached
 */
@Component
public class DmdResponseWrapper extends ResponseBodyResultHandler {

    private static final MethodParameter DMD_RESPONSE_RETURN_TYPE;

    static {
        try {
            DMD_RESPONSE_RETURN_TYPE = new MethodParameter(
                    DmdResponseWrapper.class.getDeclaredMethod("dmdResponseType"), -1);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static Mono<DmdResponse<?>> dmdResponseType() {
        return null;
    }

    public DmdResponseWrapper(ServerCodecConfigurer configurer,
                              RequestedContentTypeResolver resolver) {
        super(configurer.getWriters(), resolver);
        setOrder(50);
    }

    @Override
    public boolean supports(HandlerResult result) {
        if (!super.supports(result)) return false;
        Class<?> containingClass = result.getReturnTypeSource().getContainingClass();
        return containingClass.getPackageName().startsWith("dm.dracolich");
    }

    @Override
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        Object body = result.getReturnValue();

        if (body instanceof Flux<?> flux) {
            Mono<DmdResponse<?>> wrapped = flux.collectList().map(DmdResponse::new);
            return writeBody(wrapped, DMD_RESPONSE_RETURN_TYPE, exchange);
        }

        if (body instanceof Mono) {
            Mono<DmdResponse<?>> wrapped = Mono.from((Mono<?>) body)
                    .<DmdResponse<?>>map(this::wrapIfNeeded)
                    .defaultIfEmpty(new DmdResponse<>((Object) null));
            return writeBody(wrapped, DMD_RESPONSE_RETURN_TYPE, exchange);
        }

        return writeBody(wrapIfNeeded(body), DMD_RESPONSE_RETURN_TYPE, exchange);
    }

    private DmdResponse<?> wrapIfNeeded(Object body) {
        if (body instanceof DmdResponse<?> dmd) {
            return dmd;
        }
        return new DmdResponse<>(body);
    }
}
