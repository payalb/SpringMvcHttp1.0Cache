Expires:0 => expired in past



 HTTP caching revolves around the Cache-Control response header and, subsequently, conditional request headers (such as Last-Modified and ETag). Cache-Control advises private (for example, browser) and public (for example, proxy) caches on how to cache and re-use responses. An ETag header is used to make a conditional request that may result in a 304 (NOT_MODIFIED) without a body, if the content has not changed. ETag can be seen as a more sophisticated successor to the Last-Modified header.
 
 CacheControl provides support for configuring settings related to the Cache-Control header and is accepted as an argument in a number of places:

WebContentInterceptor

WebContentGenerator

Controllers

Static Resources

While RFC 7234 describes all possible directives for the Cache-Control response header, the CacheControl type takes a use case-oriented approach that focuses on the common scenarios:

// Cache for an hour - "Cache-Control: max-age=3600"
CacheControl ccCacheOneHour = CacheControl.maxAge(1, TimeUnit.HOURS);

// Prevent caching - "Cache-Control: no-store"
CacheControl ccNoStore = CacheControl.noStore();

// Cache for ten days in public and private caches,
// public caches should not transform the response
// "Cache-Control: max-age=864000, public, no-transform"
CacheControl ccCustom = CacheControl.maxAge(10, TimeUnit.DAYS).noTransform().cachePublic();
WebContentGenerator also accept a simpler cachePeriod property (defined in seconds) that works as follows:

A -1 value does not generate a Cache-Control response header.

A 0 value prevents caching by using the 'Cache-Control: no-store' directive.

An n > 0 value caches the given response for n seconds by using the 'Cache-Control: max-age=n' directive.


18
down vote
accepted
org.springframework.web.servlet.support.WebContentGenerator, which is the base class for all Spring controllers has quite a few methods dealing with cache headers:

The org.springframework.web.servlet.mvc.WebContentInterceptor class allows you to define default caching behaviour, plus path-specific overrides (with the same path-matcher behaviour used elsewhere). The steps for me were:

Ensure my instance of org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter does not have the "cacheSeconds" property set.
Add an instance of WebContentInterceptor:

<mvc:interceptors>
...
<bean class="org.springframework.web.servlet.mvc.WebContentInterceptor" p:cacheSeconds="0" p:alwaysUseFullPath="true" >
    <property name="cacheMappings">
        <props>
            <!-- cache for one month -->
            <prop key="/cache/me/**">2592000</prop>
            <!-- don't set cache headers -->
            <prop key="/cache/agnostic/**">-1</prop>
        </props>
    </property>
</bean>
...
</mvc:interceptors>
After these changes, responses under /foo included headers to discourage caching, responses under /cache/me included headers to encourage caching, and responses under /cache/agnostic included no cache-related headers.

If using a pure Java configuration:

@EnableWebMvc
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
  /* Time, in seconds, to have the browser cache static resources (one week). */
  private static final int BROWSER_CACHE_CONTROL = 604800;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
     .addResourceHandler("/images/**")
     .addResourceLocations("/images/")
     .setCachePeriod(BROWSER_CACHE_CONTROL);
  }
}

@Controller
public class EmployeeController {

    @RequestMapping(value = "/find/employer/{employerId}", method = RequestMethod.GET)
    public List getEmployees(@PathVariable("employerId") Long employerId, final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        return employeeService.findEmployeesForEmployer(employerId);
    }

}
In your controller, you can set response headers directly.

response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
Code above shows exactly what you want to achive. You have to do two things. Add "final HttpServletResponse response" as your parameter. And then set header Cache-Control to no-cache.


11
down vote
Starting with Spring 4.2 you can do this:

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class CachingController {
    @RequestMapping(method = RequestMethod.GET, path = "/cachedapi")
    public ResponseEntity<MyDto> getPermissions() {

        MyDto body = new MyDto();

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(20, TimeUnit.SECONDS))
            .body(body);
    }
}


CacheControl object is a builder with many configuration options


you can define a anotation for this: @CacheControl(isPublic = true, maxAge = 300, sMaxAge = 300), then render this anotation to HTTP Header with Spring MVC interceptor. or do it dynamic:

int age = calculateLeftTiming();
String cacheControlValue = CacheControlHeader.newBuilder()
      .setCacheType(CacheType.PUBLIC)
      .setMaxAge(age)
      .setsMaxAge(age).build().stringValue();
if (StringUtils.isNotBlank(cacheControlValue)) {
    response.addHeader("Cache-Control", cacheControlValue);
}


BTW: I just found that Spring MVC has build-in support for cache control: Google WebContentInterceptor or CacheControlHandlerInterceptor or CacheControl


3
down vote
I know this is a really old one, but those who are googling, this might help:

@Override
protected void addInterceptors(InterceptorRegistry registry) {

    WebContentInterceptor interceptor = new WebContentInterceptor();

    Properties mappings = new Properties();
    mappings.put("/", "2592000");
    mappings.put("/admin", "-1");
    interceptor.setCacheMappings(mappings);

    registry.addInterceptor(interceptor);
}

https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/cache-control.html

Clicking on 'test1' link will retrieve the page from browser's local cache, but clicking on reload button (or F5) will reload the page from server. This happens if we are on the same tab. If we open new tab or new browser window and enter the url in the address bar, the page will be retrieved from the cache. This behavior is consistent in the latest versions of Chrome, FireFox and Edge browsers.
Default Cache-Control
According to the specification, if there's no cache-control header specified by the response, the browser will always cache the contents. It will probably be cached for a long period or may be for good, unless user uses F5 or Ctrl+F5 (hard refresh) or clears the cache manually by using browser settings.

https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9

 Cache-Control   = "Cache-Control" ":" 1#cache-directive
    cache-directive = cache-request-directive
         | cache-response-directive
    cache-request-directive =
           "no-cache"                          ; Section 14.9.1
         | "no-store"                          ; Section 14.9.2
         | "max-age" "=" delta-seconds         ; Section 14.9.3, 14.9.4
         | "max-stale" [ "=" delta-seconds ]   ; Section 14.9.3
         | "min-fresh" "=" delta-seconds       ; Section 14.9.3
         | "no-transform"                      ; Section 14.9.5
         | "only-if-cached"                    ; Section 14.9.4
         | cache-extension                     ; Section 14.9.6
     cache-response-directive =
           "public"                               ; Section 14.9.1
         | "private" [ "=" <"> 1#field-name <"> ] ; Section 14.9.1
         | "no-cache" [ "=" <"> 1#field-name <"> ]; Section 14.9.1
         | "no-store"                             ; Section 14.9.2
         | "no-transform"                         ; Section 14.9.5
         | "must-revalidate"                      ; Section 14.9.4
         | "proxy-revalidate"                     ; Section 14.9.4
         | "max-age" "=" delta-seconds            ; Section 14.9.3
         | "s-maxage" "=" delta-seconds           ; Section 14.9.3
         | cache-extension                        ; Section 14.9.6
    cache-extension = token [ "=" ( token | quoted-string ) ]
When a directive appears without any 1#field-name parameter, the directive applies to the entire request or response. When such a directive appears with a 1#field-name parameter, it applies only to the named field or fields, and not to the rest of the request or response. This mechanism supports extensibility; implementations of future versions of the HTTP protocol might apply these directives to header fields not defined in HTTP/1.1.

@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // This is just an example
    registry.addResourceHandler("/img/**")
            .addResourceLocations("classpath:/static/images/")
            .setCachePeriod(12);
}
We could also override the Spring Security default settings. If we want to deactivate the "no cache control" policy for our API, we can change the ApiSecurityConfiguration class...

https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/cache-control.html