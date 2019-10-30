<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script type="text/javascript">
    (function () {
        var environment = '<c:out value="${env.code}" />',
            type = '<c:out value="${env.type}" />',
            container = document.createElement("div");

        if (environment && type) {
            container.id = 'mpern-env-ribbon';
            container.classList.add("mpern-env-ribbon");
            container.dataset.environment = environment;
            container.dataset.type = type;

            document.body.insertBefore(container, document.body.firstChild);
        }
    })();
</script>