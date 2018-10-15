<%@page isELIgnored="false" import="java.time.LocalDateTime"%>
${message}

<%=LocalDateTime.now()%>

<a href="./caching">Data</a>

<!-- Clicking on 'test1' link will retrieve the page from browser's local cache, but clicking on reload button (or F5) will reload the page from server. This happens if we are on the same tab. If we open new tab or new browser window and enter the url in the address bar, the page will be retrieved from the cache. This behavior is consistent in the latest versions of Chrome, FireFox and Edge browsers. -->