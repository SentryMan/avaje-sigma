package io.avaje.sigma.routes;

import io.avaje.sigma.ExceptionHandler;
import io.avaje.sigma.Router;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RoutesBuilder {

  private final EnumMap<Router.HttpMethod, RouteIndex> typeMap =
      new EnumMap<>(Router.HttpMethod.class);
  private final List<SpiRoutes.Entry> before = new ArrayList<>();
  private final List<SpiRoutes.Entry> after = new ArrayList<>();
  private final boolean ignoreTrailingSlashes;
  private final Map<Class<?>, ExceptionHandler<?>> exceptionHandlers;

  public RoutesBuilder(Router routing, boolean ignoreTrailingSlashes) {
    this.exceptionHandlers = routing.exceptionHandlers();
    this.ignoreTrailingSlashes = ignoreTrailingSlashes;
    for (Router.Entry handler : routing.all()) {
      switch (handler.type()) {
        case BEFORE:
          before.add(filter(handler));
          break;
        case AFTER:
          after.add(filter(handler));
          break;
        default:
          typeMap.computeIfAbsent(handler.type(), h -> new RouteIndex()).add(convert(handler));
      }
    }
  }

  private FilterEntry filter(Router.Entry entry) {
    return new FilterEntry(entry, ignoreTrailingSlashes);
  }

  private SpiRoutes.Entry convert(Router.Entry handler) {
    final PathParser pathParser = new PathParser(handler.path(), ignoreTrailingSlashes);
    return new RouteEntry(pathParser, handler.handler());
  }

  public SpiRoutes build() {
    return new Routes(typeMap, before, after, exceptionHandlers);
  }
}
